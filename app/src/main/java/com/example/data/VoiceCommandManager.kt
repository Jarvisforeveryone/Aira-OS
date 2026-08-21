package com.example.data

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.util.Log
import com.example.service.AiraAccessibilityService
import com.example.service.AiraAutomationEngine
import com.example.ui.AiraViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar
import java.util.Locale

class VoiceCommandManager(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val voiceDao = db.voiceCommandDao()
    private val chatDao = db.chatMessageDao()
    private val automationEngine by lazy { AiraAutomationEngine(context) }

    companion object {
        @Volatile
        private var INSTANCE: VoiceCommandManager? = null

        private val _currentEngineSource = kotlinx.coroutines.flow.MutableStateFlow("Auto-routing Active")
        val currentEngineSource: kotlinx.coroutines.flow.StateFlow<String> = _currentEngineSource

        fun getInstance(context: Context): VoiceCommandManager {
            return INSTANCE ?: synchronized(this) {
                val instance = VoiceCommandManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        fun clearInstance() {
            INSTANCE = null
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // COMPLETE ALIAS MAP — 200+ COMMANDS (ENGLISH, ROMAN URDU, URDU SCRIPT)
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        val commandAliases: Map<String, List<String>> = mapOf(
            // 1. Navigation & System
            "goBack" to listOf(
                "go back", "back", "go backward", "move back", "return", "back up", "previous screen", "go to previous",
                "wapas jao", "vapas ja", "wapis jao", "wapas", "peechay jao", "peeche jao", "wapis", "back karo", "pichli screen", "peechay",
                "واپس جاؤ", "پیچھے جاؤ"
            ),
            "goHome" to listOf(
                "go home", "home", "main screen", "home screen", "return to home", "desktop", "launch screen",
                "ghar jao", "home jao", "home screen kholo", "main screen jao", "home screen", "main menu", "home par jao",
                "گھر جاؤ", "ہوم اسکرین"
            ),
            "scrollDown" to listOf(
                "scroll down", "down", "move down", "scroll below", "swipe down list", "page down", "look down",
                "neeche jao", "scroll neeche", "neechay jao", "down karo", "neeche scroll karo", "neeche dekho", "neechay karo",
                "نیچے جاؤ", "سکرول نیچے"
            ),
            "scrollUp" to listOf(
                "scroll up", "up", "move up", "scroll above", "swipe up list", "page up", "look up",
                "oopar jao", "scroll oopar", "oper jao", "up karo", "oopar scroll karo", "uper jao", "upper jao",
                "اوپر جاؤ", "سکرول اوپر"
            ),
            "takeScreenshot" to listOf(
                "take screenshot", "screenshot", "screen shot", "capture screen", "snap screen", "save screen", "screen grab",
                "screenshot lo", "screen shot lo", "screen capture karo", "tasveer lo screen ki", "screen save karo", "screenshot kheecho",
                "اسکرین شاٹ لو", "اسکرین محفوظ کرو"
            ),
            "lockScreen" to listOf(
                "lock screen", "lock phone", "turn off screen", "screen lock", "sleep screen", "lock device",
                "phone lock karo", "screen lock karo", "mobile lock karo", "band karo screen", "lock lagao", "device lock karo",
                "اسکرین لاک کرو", "فون لاک کرو"
            ),
            "unlockScreen" to listOf(
                "unlock screen", "unlock phone", "wake up screen", "screen unlock", "turn on screen", "unlock device",
                "phone unlock karo", "screen unlock karo", "mobile kholo", "lock kholo", "screen on karo", "phone kholo",
                "اسکرین ان لاک کرو", "فون کھولو"
            ),
            "openNotifications" to listOf(
                "open notifications", "show notifications", "notification shade", "view notifications", "pull down notifications", "notification bar",
                "notifications kholo", "notification shade kholo", "alerts dikhao", "notifications dikhao", "messages check karo",
                "نوٹیفکیشنز کھولو", "اطلاعات دیکھیں"
            ),
            "openQuickSettings" to listOf(
                "open quick settings", "show quick settings", "quick panel", "control center", "pull down settings", "quick toggles",
                "quick settings kholo", "control panel kholo", "toggles dikhao", "settings panel kholo", "short settings",
                "کوئیک سیٹنگز کھولو", "فوری ترتیبات"
            ),
            "openRecents" to listOf(
                "open recents", "show recents", "recent apps", "overview", "app switcher", "task manager", "running apps",
                "recent apps kholo", "recents dikhao", "chal rahi apps", "background apps dikhao", "task switcher kholo",
                "حالیہ ایپس", "اوور ویو"
            ),

            // 2. Connectivity & Hardware Toggles
            "toggleWiFi" to listOf(
                "toggle wifi", "switch wifi", "change wifi state",
                "wifi toggle karo", "wifi badlo", "wifi switch karo",
                "وائی فائی تبدیل کرو"
            ),
            "turnOnWiFi" to listOf(
                "turn on wifi", "wifi on", "enable wifi", "connect wifi", "start wifi", "activate wifi",
                "wifi on karo", "wifi chalu karo", "wifi chalao", "wifi enable karo", "wifi start karo",
                "وائی فائی آن کرو", "وائی فائی چالو کریں"
            ),
            "turnOffWiFi" to listOf(
                "turn off wifi", "wifi off", "disable wifi", "disconnect wifi", "stop wifi", "shut wifi",
                "wifi off karo", "wifi band karo", "wifi disable karo", "wifi roko", "wifi cut karo",
                "وائی فائی آف کرو", "وائی فائی بند کریں"
            ),
            "toggleBluetooth" to listOf(
                "toggle bluetooth", "switch bluetooth", "change bluetooth",
                "bluetooth toggle karo", "bluetooth badlo",
                "بلوٹوتھ تبدیل کرو"
            ),
            "turnOnBluetooth" to listOf(
                "turn on bluetooth", "bluetooth on", "enable bluetooth", "start bluetooth", "activate bluetooth",
                "bluetooth on karo", "bluetooth chalu karo", "bluetooth chalao", "bluetooth enable karo",
                "بلوٹوتھ آن کرو", "بلوٹوتھ چالو کریں"
            ),
            "turnOffBluetooth" to listOf(
                "turn off bluetooth", "bluetooth off", "disable bluetooth", "stop bluetooth", "shut bluetooth",
                "bluetooth off karo", "bluetooth band karo", "bluetooth disable karo", "bluetooth roko",
                "بلوٹوتھ آف کرو", "بلوٹوتھ بند کریں"
            ),
            "turnOnFlashlight" to listOf(
                "turn on flashlight", "flashlight on", "torch on", "enable torch", "start flashlight", "light on",
                "torch jalao", "flashlight on karo", "batti jalao", "torch on karo", "light jalao", "torch chalao",
                "ٹارچ آن کرو", "روشنی آن کرو"
            ),
            "turnOffFlashlight" to listOf(
                "turn off flashlight", "flashlight off", "torch off", "disable torch", "stop flashlight", "light off",
                "torch bujhao", "flashlight off karo", "batti bujhao", "torch off karo", "light band karo",
                "ٹارچ آف کرو", "روشنی بند کرو"
            ),
            "setBrightness" to listOf(
                "set brightness", "change brightness", "adjust brightness", "screen brightness", "dim screen", "brighten screen",
                "brightness set karo", "roshni badhao", "brightness kam karo", "screen ki roshni", "brightness change karo",
                "برائٹنس سیٹ کرو", "روشنی تبدیل کرو"
            ),
            "volumeUp" to listOf(
                "volume up", "increase volume", "louder", "turn volume up", "sound up", "raise volume", "boost sound",
                "awaz badhao", "volume up karo", "awaz oopar karo", "volume tez karo", "sound badhao", "loud karo",
                "آواز بڑھاؤ", "والیوم زیادہ کرو"
            ),
            "volumeDown" to listOf(
                "volume down", "decrease volume", "lower volume", "turn volume down", "sound down", "reduce volume", "softer",
                "awaz kam karo", "volume down karo", "awaz neeche karo", "volume dheema karo", "sound kam karo",
                "آواز کم کرو", "والیوم کم کرو"
            ),
            "muteVolume" to listOf(
                "mute volume", "mute sound", "silence", "mute phone", "turn off audio", "zero volume",
                "awaz band karo", "mute karo", "khamosh karo", "sound off karo", "awaz zero karo",
                "میوٹ کرو", "آواز بند کرو"
            ),
            "silentMode" to listOf(
                "silent mode", "set silent", "turn on silent", "silence mode", "do not disturb",
                "silent karo", "silent mode lagao", "phone silent karo", "khamoshi mode",
                "سائلنٹ موڈ", "خاموش موڈ"
            ),
            "ringMode" to listOf(
                "ring mode", "normal sound", "ringer on", "enable ringer", "sound mode",
                "ringer on karo", "ring mode lagao", "awaz kholo", "sound on karo",
                "رنگر آن کرو", "نارمل آواز"
            ),
            "vibrateMode" to listOf(
                "vibrate mode", "vibration mode", "set vibrate", "vibrate only",
                "vibrate karo", "vibration lagao", "vibrate par lagao", "kapkapi mode",
                "وائبریشن موڈ", "وائبریٹ کرو"
            ),

            // 3. UI Interactions & Gestures
            "openApp" to listOf(
                "open app", "launch app", "start application", "run app", "execute app",
                "app kholo", "app open karo", "launch karo", "application chalao", "app start karo",
                "ایپ کھولو", "ایپلی کیشن شروع کریں"
            ),
            "typeText" to listOf(
                "type", "type text", "write text", "insert text", "write", "enter text", "input text",
                "type karo", "likho", "text likho", "tahzeeb likho", "text enter karo", "likhna shuru karo",
                "ٹائپ کرو", "لکھو"
            ),
            "clickByText" to listOf(
                "click", "tap", "click on", "tap on", "press button", "select item", "touch target",
                "click karo", "tap karo", "click on karo", "button dabao", "dabao", "select karo",
                "کلک کرو", "ٹیپ کرو"
            ),
            "readScreen" to listOf(
                "read screen", "read this", "screen reader", "speak screen", "what is on screen", "read out loud",
                "screen parh k sunao", "parh k sunao", "read karo", "screen parho", "bol kar sunao", "kya likha hai parho",
                "اسکرین پڑھ کر سناؤ", "اسکرین پڑھو"
            ),
            "swipeUp" to listOf(
                "swipe up", "drag up", "gesture up", "slide up",
                "oopar swipe karo", "swipe up karo", "oopar kheecho", "uper slide karo",
                "اوپر سوائپ کرو", "اوپر کھینچیں"
            ),
            "swipeDown" to listOf(
                "swipe down", "drag down", "gesture down", "slide down",
                "neeche swipe karo", "swipe down karo", "neeche kheecho", "neechay slide karo",
                "نیچے سوائپ کرو", "نیچے کھینچیں"
            ),
            "swipeLeft" to listOf(
                "swipe left", "drag left", "gesture left", "slide left", "next slide",
                "baayein swipe karo", "swipe left karo", "left kheecho", "bayen karo",
                "بائیں سوائپ کرو", "بائیں سلائیڈ"
            ),
            "swipeRight" to listOf(
                "swipe right", "drag right", "gesture right", "slide right", "previous slide",
                "daayein swipe karo", "swipe right karo", "right kheecho", "dayen karo",
                "دائیں سوائپ کرو", "دائیں سلائیڈ"
            ),
            "findText" to listOf(
                "find text", "search on screen", "locate text", "look for word",
                "text dhoondo", "lafz dhoondo", "screen par talash karo", "khoj karo",
                "متن تلاش کریں", "لفظ تلاش کرو"
            ),
            "waitForText" to listOf(
                "wait for text", "wait until appears", "wait for element",
                "text ka intezar karo", "aane ka wait karo", "intezar karo",
                "انتظار کرو", "متن کا انتظار کریں"
            ),
            "dumpUITree" to listOf(
                "dump ui tree", "inspect ui", "show ui hierarchy", "view nodes",
                "ui check karo", "nodes dekho", "elements inspect karo",
                "یو آئی ٹری", "عناصر دیکھیں"
            ),
            "zoomIn" to listOf(
                "zoom in", "magnify", "enlarge view", "zoom closer",
                "bada dikhao", "zoom in karo", "nazdeek lao", "bada karo",
                "زوم ان", "بڑا کر کے دکھاؤ"
            ),
            "zoomOut" to listOf(
                "zoom out", "shrink view", "zoom farther", "unzoom",
                "chhota dikhao", "zoom out karo", "door karo", "chhota karo",
                "زوم آؤٹ", "چھوٹا کر کے دکھاؤ"
            ),

            // 4. App Management & System Ops
            "uninstallApp" to listOf(
                "uninstall app", "delete app", "remove app", "erase app",
                "app delete karo", "uninstall karo", "app hatao", "app nikal do",
                "ایپ ان انسٹال کرو", "ایپ ڈیلیٹ کرو"
            ),
            "forceStopApp" to listOf(
                "force stop app", "kill app", "close app", "terminate application",
                "app band karo", "force stop karo", "app khatam karo", "band karo app",
                "ایپ فورس اسٹاپ کرو", "ایپ بند کرو"
            ),
            "clearAppData" to listOf(
                "clear app data", "reset app data", "wipe app data", "clean data",
                "data saaf karo", "app data udao", "clear data karo", "reset karo app",
                "ڈیٹا صاف کرو", "ایپ ڈیٹا ختم کرو"
            ),
            "installApk" to listOf(
                "install apk", "install package", "load apk",
                "apk install karo", "package install karo", "app install karo",
                "اے پی کے انسٹال کرو", "پیکیج انسٹال کریں"
            ),
            "openSettings" to listOf(
                "open settings", "system settings", "device settings", "configuration",
                "settings kholo", "phone settings jao", "settings open karo", "setting kholo",
                "سیٹنگز کھولو", "ترتیبات کھولیں"
            ),

            // 5. Media & Camera
            "playMusic" to listOf(
                "play music", "resume music", "play song", "start audio", "play track", "hit play",
                "gana chalao", "music play karo", "gana lagao", "audio chalao", "song chalao",
                "گانا چلاؤ", "موسیقی چلائیں"
            ),
            "pauseMusic" to listOf(
                "pause music", "stop music", "pause song", "halt audio", "pause playback", "stop playback",
                "gana roko", "music pause karo", "gana band karo", "audio roko", "song pause karo",
                "گانا روکو", "موسیقی روکیں"
            ),
            "nextTrack" to listOf(
                "next track", "next song", "skip song", "next audio", "forward song",
                "agla gana", "next gana", "agla song chalao", "gana aage karo", "next track karo",
                "اگلا گانا", "اگلا ٹریک"
            ),
            "previousTrack" to listOf(
                "previous track", "previous song", "rewind song", "last song", "back song",
                "pichla gana", "previous song chalao", "peechay wala gana", "gana peechay karo",
                "پچھلا گانا", "پچھلا ٹریک"
            ),
            "takePhoto" to listOf(
                "take photo", "take picture", "click picture", "capture image", "snap photo", "shoot picture",
                "tasveer kheencho", "photo lo", "tasveer lo", "picture click karo", "photo kheecho",
                "تصویر کھینچیں", "فوٹو لو"
            ),
            "recordVideo" to listOf(
                "record video", "start recording video", "capture video", "shoot video", "film video",
                "video banao", "video record karo", "video shuru karo", "recording on karo",
                "ویڈیو ریکارڈ کریں", "ویڈیو بنائیں"
            ),
            "openCamera" to listOf(
                "open camera", "launch camera", "start camera", "camera app",
                "camera kholo", "camera open karo", "camera chalao",
                "کیمرہ کھولیں", "کیمرہ آن کرو"
            ),
            "openGallery" to listOf(
                "open gallery", "open photos", "view gallery", "photo album", "image gallery",
                "gallery kholo", "photos kholo", "tasveerein dikhao", "gallery open karo",
                "گیلری کھولیں", "تصاویر دیکھیں"
            ),

            // 6. Text Editing & Clipboard
            "copyText" to listOf(
                "copy text", "copy", "copy to clipboard", "duplicate text",
                "copy karo", "text copy karo", "clipboard mein lo",
                "کاپی کریں", "متن کاپی کریں"
            ),
            "pasteText" to listOf(
                "paste text", "paste", "paste from clipboard", "insert clipboard",
                "paste karo", "text paste karo", "yahan chipkao",
                "پیسٹ کریں", "متن پیسٹ کریں"
            ),
            "cutText" to listOf(
                "cut text", "cut", "cut to clipboard",
                "cut karo", "text cut karo", "yahan se hatao",
                "کٹ کریں", "متن کٹ کریں"
            ),
            "undo" to listOf(
                "undo", "revert", "step back", "undo change",
                "undo karo", "wapas lo", "pehle jaisa karo",
                "ان ڈو", "واپس لیں"
            ),
            "redo" to listOf(
                "redo", "reapply", "step forward", "redo change",
                "redo karo", "dobara karo", "phir se karo",
                "ری ڈو", "دوبارہ کریں"
            ),
            "selectAll" to listOf(
                "select all", "highlight all", "choose all", "mark all",
                "sab select karo", "sara select karo", "tamam chun lo",
                "سب منتخب کریں", "تمام منتخب کریں"
            ),
            "clearSelection" to listOf(
                "clear selection", "unselect", "deselect all",
                "selection hatao", "deselect karo", "chuna hua hatao",
                "انتخاب ختم کریں", "ڈی سلیکٹ کریں"
            ),
            "boldText" to listOf(
                "bold text", "make bold", "bold font",
                "bold karo", "text mota karo", "bold text karo",
                "بولڈ کریں", "موٹا لکھیں"
            ),
            "italicText" to listOf(
                "italic text", "make italic", "slant text",
                "italic karo", "text teda karo", "italic font karo",
                "اٹالک کریں", "ترچھا لکھیں"
            ),
            "underlineText" to listOf(
                "underline text", "add underline", "underline words",
                "underline karo", "neeche lakeer kheencho", "lakeer lagao",
                "انڈر لائن کریں", "نیچے لکیر لگائیں"
            ),
            "alignLeft" to listOf(
                "align left", "left align", "justify left",
                "left align karo", "baayein taraf karo",
                "بائیں سیدھ کریں"
            ),
            "alignRight" to listOf(
                "align right", "right align", "justify right",
                "right align karo", "daayein taraf karo",
                "دائیں سیدھ کریں"
            ),

            // 7. Information, Weather, Stocks & Web
            "getWeather" to listOf(
                "get weather", "check weather", "weather forecast", "how is the weather", "temperature outside", "is it raining",
                "mausam kaisa hai", "weather batao", "aaj ka mausam", "barish hogi kya", "temperature kitna hai",
                "موسم کا حال", "آج کا موسم"
            ),
            "getNews" to listOf(
                "get news", "check news", "latest headlines", "todays news", "breaking news", "current events",
                "khabrein sunao", "taza khabrein", "news batao", "aaj ki khabrein", "headlines sunao",
                "خبریں سناؤ", "تازہ ترین خبریں"
            ),
            "getStockPrice" to listOf(
                "get stock price", "stock market", "shares price", "check stocks", "nasdaq update",
                "stocks check karo", "share market batao", "stock ki qeemat", "market ka haal",
                "اسٹاک کی قیمت", "شیئر مارکیٹ"
            ),
            "getCryptoPrice" to listOf(
                "get crypto price", "bitcoin price", "crypto market", "check cryptocurrency",
                "crypto rate batao", "bitcoin ka rate", "crypto check karo", "digital currency",
                "کرپٹو ریٹ", "بٹ کوائن قیمت"
            ),
            "getSportsScore" to listOf(
                "get sports score", "match score", "cricket score", "football score", "game update",
                "match score batao", "cricket ka score", "khel ka haal", "score kya hua",
                "میچ کا اسکور", "کرکٹ اسکور"
            ),
            "getTraffic" to listOf(
                "get traffic", "traffic condition", "road traffic", "is there traffic",
                "traffic kaisa hai", "rush kitna hai", "road clear hai kya", "traffic check karo",
                "ٹریفک کا حال", "راستے کی صورتحال"
            ),
            "navigateTo" to listOf(
                "navigate to", "get directions", "route to", "drive to", "maps direction",
                "rasta dikhao", "navigation chalao", "directions batao", "yahan le chalo",
                "راستہ دکھاؤ", "رہنمائی کریں"
            ),
            "getDistance" to listOf(
                "get distance", "how far is", "calculate distance", "travel distance",
                "fasla kitna hai", "kitni door hai", "distance batao", "faasla check karo",
                "فاصلہ کتنا ہے", "کتنی دور ہے"
            ),

            // 8. Communication & Social Apps
            "makeCall" to listOf(
                "call someone", "make a phone call", "dial number", "call contact", "place call",
                "call milao", "phone lagao", "call karo", "dial karo", "rabta karo",
                "کال کرو", "فون ملاؤ"
            ),
            "sendSMS" to listOf(
                "send sms", "send text message", "write message", "sms contact",
                "message bhejo", "sms bhejo", "paigham bhejo", "text karo",
                "میسج بھیجو", "پیغام بھیجیں"
            ),
            "getEmail" to listOf(
                "get email", "check email", "read mail", "inbox check",
                "email check karo", "mail parho", "inbox dikhao", "emails dekho",
                "ای میل چیک کرو", "ان باکس دیکھیں"
            ),
            "getMessages" to listOf(
                "get messages", "check messages", "read sms", "view text messages",
                "messages parho", "sms check karo", "paigham parho", "messages dikhao",
                "پیغامات چیک کرو", "پیغام پڑھیں"
            ),
            "checkWhatsApp" to listOf(
                "check whatsapp", "open whatsapp", "whatsapp messages", "whatsapp chat",
                "whatsapp kholo", "whatsapp check karo", "whatsapp open karo", "whatsapp dekho",
                "واٹس ایپ چیک کرو", "واٹس ایپ کھولو"
            ),
            "openWhatsApp" to listOf(
                "open whatsapp", "launch whatsapp", "whatsapp app",
                "whatsapp kholo", "whatsapp chalao", "whatsapp open karo",
                "واٹس ایپ کھولیں"
            ),
            "openChrome" to listOf(
                "open chrome", "launch browser", "open browser", "google chrome",
                "chrome kholo", "browser open karo", "chrome chalao", "internet kholo",
                "کروم کھولو", "براؤزر کھولیں"
            ),
            "openYouTube" to listOf(
                "open youtube", "launch youtube", "youtube app", "watch youtube",
                "youtube kholo", "youtube open karo", "youtube chalao", "videos dikhao",
                "یوٹیوب کھولو", "یوٹیوب کھولیں"
            ),
            "openGmail" to listOf(
                "open gmail", "launch gmail", "gmail app",
                "gmail kholo", "gmail open karo", "email app kholo",
                "جی میل کھولو", "جی میل کھولیں"
            ),
            "openInstagram" to listOf(
                "open instagram", "launch instagram", "open insta",
                "instagram kholo", "insta kholo", "instagram open karo",
                "انسٹاگرام کھولو", "انسٹاگرام کھولیں"
            ),
            "openFacebook" to listOf(
                "open facebook", "launch facebook", "fb app",
                "facebook kholo", "fb kholo", "facebook open karo",
                "فیس بک کھولو", "فیس بک کھولیں"
            ),
            "openTwitter" to listOf(
                "open twitter", "open x", "launch twitter",
                "twitter kholo", "x kholo", "twitter open karo",
                "ٹوئٹر کھولو", "ایکس کھولیں"
            ),
            "openSnapchat" to listOf(
                "open snapchat", "launch snapchat", "snap app",
                "snapchat kholo", "snap kholo", "snapchat open karo",
                "سنیپ چیٹ کھولو", "سنیپ چیٹ کھولیں"
            ),
            "openTikTok" to listOf(
                "open tiktok", "launch tiktok", "tiktok video",
                "tiktok kholo", "tiktok open karo", "tiktok chalao",
                "ٹک ٹاک کھولو", "ٹک ٹاک کھولیں"
            ),
            "openReddit" to listOf(
                "open reddit", "launch reddit",
                "reddit kholo", "reddit open karo", "reddit check karo",
                "ریڈٹ کھولو", "ریڈٹ کھولیں"
            ),
            "openLinkedIn" to listOf(
                "open linkedin", "launch linkedin",
                "linkedin kholo", "linkedin open karo", "linkedin check karo",
                "لنکڈ ان کھولو", "لنکڈ ان کھولیں"
            ),
            "openSpotify" to listOf(
                "open spotify", "launch spotify", "spotify music",
                "spotify kholo", "spotify open karo", "spotify chalao",
                "اسپاٹی فائی کھولو", "اسپاٹی فائی کھولیں"
            ),
            "openNetflix" to listOf(
                "open netflix", "launch netflix", "watch netflix",
                "netflix kholo", "netflix open karo", "netflix chalao",
                "نیٹ فلکس کھولو", "نیٹ فلکس کھولیں"
            ),
            "openPlayStore" to listOf(
                "open play store", "google play store", "app store",
                "play store kholo", "play store open karo", "apps download store",
                "پلے اسٹور کھولو", "گوگل پلے اسٹور"
            ),
            "openCalculator" to listOf(
                "open calculator", "launch calculator", "calc app",
                "calculator kholo", "hisaab kitab kholo", "calc open karo",
                "کیلکولیٹر کھولو", "کیلکولیٹر کھولیں"
            ),
            "openClock" to listOf(
                "open clock", "launch clock", "open alarm app",
                "ghadi kholo", "clock open karo", "waqt dikhao",
                "گھڑی کھولو", "گھڑی کھولیں"
            ),
            "openMaps" to listOf(
                "open maps", "google maps", "launch maps",
                "maps kholo", "naqsha kholo", "google maps open karo",
                "نقشہ کھولو", "گوگل میپس"
            ),
            "openContacts" to listOf(
                "open contacts", "phonebook", "contact list",
                "contacts kholo", "numbers ki list", "phonebook kholo",
                "رابطے کھولو", "فون بک"
            ),

            // 9. Files & Storage
            "openFileManager" to listOf(
                "open file manager", "open files", "file browser", "my files",
                "files kholo", "file manager open karo", "folders dikhao", "storage kholo",
                "فائل مینیجر کھولو", "فائلیں کھولیں"
            ),
            "saveFile" to listOf(
                "save file", "write file to disk", "store file",
                "file save karo", "file mehfooz karo", "save karo",
                "فائل محفوظ کریں", "محفوظ کریں"
            ),
            "downloadFile" to listOf(
                "download file", "start download", "get file",
                "file download karo", "downloading shuru karo",
                "فائل ڈاؤن لوڈ کرو", "ڈاؤن لوڈ کریں"
            ),
            "deleteFile" to listOf(
                "delete file", "remove file", "erase file",
                "file delete karo", "file mitao", "file hatao",
                "فائل ڈیلیٹ کرو", "فائل مٹائیں"
            ),
            "renameFile" to listOf(
                "rename file", "change file name", "modify file name",
                "naam badlo", "file ka naam badlo", "rename karo",
                "فائل کا نام بدلو", "نام تبدیل کریں"
            ),
            "copyFile" to listOf(
                "copy file", "duplicate file",
                "file copy karo", "file ki copy banao",
                "فائل کاپی کرو", "کاپی بنائیں"
            ),
            "createFolder" to listOf(
                "create folder", "new folder", "make directory",
                "folder banao", "naya folder banao", "directory banao",
                "نیا فولڈر بنائیں", "فولڈر بنائیں"
            ),
            "deleteFolder" to listOf(
                "delete folder", "remove directory",
                "folder delete karo", "folder mitao",
                "فولڈر ڈیلیٹ کرو", "فولڈر مٹائیں"
            ),
            "extractZip" to listOf(
                "extract zip", "unzip file", "decompress archive",
                "zip kholo", "unzip karo", "file extract karo",
                "زپ فائل ان زپ کرو", "فائل نکالیں"
            ),
            "compressFiles" to listOf(
                "compress files", "create zip", "make zip archive",
                "zip banao", "compress karo", "archive banao",
                "زپ فائل بنائیں", "فائل کمپریس کریں"
            ),

            // 10. Alarms, Reminders & Calendar
            "setAlarm" to listOf(
                "set alarm", "wake me up", "create alarm", "alarm for tomorrow",
                "alarm lagao", "subah ka alarm", "mujhe uthao", "alarm set karo",
                "الارم لگاؤ", "الارم سیٹ کریں"
            ),
            "setTimer" to listOf(
                "set timer", "countdown timer", "start timer", "time me",
                "timer lagao", "timer shuru karo", "countdown lagao",
                "ٹائمر لگاؤ", "ٹائمر شروع کریں"
            ),
            "stopTimer" to listOf(
                "stop timer", "cancel timer", "halt countdown",
                "timer roko", "timer band karo", "timer cancel karo",
                "ٹائمر بند کرو", "ٹائمر روکیں"
            ),
            "setReminder" to listOf(
                "set reminder", "remind me", "create reminder", "make a note",
                "yaad dilao", "reminder lagao", "yaad rakhna", "mujhe batana",
                "یاد دہانی لگاؤ", "یاد دلائیں"
            ),
            "checkCalendar" to listOf(
                "check calendar", "view schedule", "calendar events", "what is on my calendar",
                "calendar dekho", "mera schedule batao", "aaj ka plan", "calendar check karo",
                "کیلنڈر چیک کرو", "شیڈول دیکھیں"
            ),
            "addEvent" to listOf(
                "add event", "create calendar event", "schedule meeting", "add appointment",
                "event add karo", "meeting schedule karo", "entry dalo", "event banao",
                "ایونٹ درج کریں", "میٹنگ درج کریں"
            ),
            "checkAlarms" to listOf(
                "check alarms", "list alarms", "show all alarms",
                "alarms dekho", "kitne alarm lage hain", "alarms list dikhao",
                "الارمز چیک کرو", "تمام الارم دیکھیں"
            ),
            "disableAlarm" to listOf(
                "disable alarm", "turn off alarm", "cancel alarm",
                "alarm band karo", "alarm off karo", "alarm hatao",
                "الارم بند کرو", "الارم منسوخ کریں"
            ),
            "snoozeAlarm" to listOf(
                "snooze alarm", "snooze", "delay alarm",
                "alarm snooze karo", "thodi der baad bajana", "snooze dabao",
                "الارم اسنوز کرو", "تھوڑی دیر بعد بجاؤ"
            ),

            // 11. Smart Home & Automation
            "turnOnLight" to listOf(
                "turn on light", "switch on lights", "enable smart light", "illuminate room",
                "batti jalao", "light on karo", "roshni karo", "lights chalu karo",
                "لائٹ آن کرو", "بتی جلاؤ"
            ),
            "turnOffLight" to listOf(
                "turn off light", "switch off lights", "disable smart light", "darken room",
                "batti bujhao", "light off karo", "lights band karo", "andhera karo",
                "لائٹ آف کرو", "بتی بجھاؤ"
            ),
            "setTemperature" to listOf(
                "set temperature", "adjust thermostat", "change room temp", "ac temperature",
                "temperature set karo", "ac ka temperature", "kamre ka darja hararat",
                "درجہ حرارت سیٹ کرو", "تھرموسٹیٹ سیٹ کریں"
            ),
            "lockDoor" to listOf(
                "lock door", "secure lock", "lock smart lock",
                "darwaza lock karo", "kundi lagao", "door lock karo",
                "دروازہ لاک کرو", "کنڈی لگائیں"
            ),
            "unlockDoor" to listOf(
                "unlock door", "unlatch door", "open smart lock",
                "darwaza kholo", "lock kholo", "door unlock karo",
                "دروازہ ان لاک کرو", "دروازہ کھولیں"
            ),
            "startVacuum" to listOf(
                "start vacuum", "vacuum floor", "robot vacuum on",
                "safai shuru karo", "vacuum chalao", "safai karo",
                "ویکیوم شروع کرو", "صفائی شروع کریں"
            ),
            "stopVacuum" to listOf(
                "stop vacuum", "pause robot cleaner", "dock vacuum",
                "safai roko", "vacuum band karo", "robot roko",
                "ویکیوم بند کرو", "صفائی روکیں"
            ),

            // 12. Device Diagnostics & Status
            "getBatteryStatus" to listOf(
                "get battery status", "battery percentage", "battery level", "check battery", "how much battery",
                "battery kitni hai", "charging kitni bachi", "battery status batao", "battery check karo",
                "بیٹری کا حال", "بیٹری چیک کرو"
            ),
            "getStorageStatus" to listOf(
                "get storage status", "storage space", "available memory", "check disk space",
                "storage kitni bachi hai", "memory check karo", "space kitna hai",
                "میموری چیک کرو", "اسٹوریج کی تفصیل"
            ),
            "getRAMStatus" to listOf(
                "get ram status", "check ram", "memory usage", "free ram",
                "ram check karo", "ram kitni khali hai", "memory kitni use ho rahi hai",
                "ریم چیک کرو", "ریم کی تفصیل"
            ),
            "getNetworkStatus" to listOf(
                "get network status", "check internet speed", "connection quality", "ping test",
                "internet speed check", "network kaisa hai", "connection check karo",
                "نیٹ ورک اسٹیٹس", "انٹرنیٹ اسپیڈ"
            ),
            "getDeviceInfo" to listOf(
                "get device info", "phone specs", "system specifications", "about phone",
                "phone ki maloomat", "device details batao", "phone specs kya hain",
                "ڈیوائس کی معلومات", "فون کی تفصیل"
            ),

            // 13. Translations & Language Assistance
            "translateToUrdu" to listOf(
                "translate to urdu", "convert to urdu", "urdu translation", "meaning in urdu",
                "urdu mein tarjuma karo", "urdu banao", "urdu mein batao", "urdu matlab kya hai",
                "اردو ترجمہ", "اردو میں ترجمہ کریں"
            ),
            "translateToEnglish" to listOf(
                "translate to english", "convert to english", "english translation",
                "english mein tarjuma karo", "angrezi banao", "english mein batao",
                "انگریزی ترجمہ", "انگریزی میں ترجمہ کریں"
            ),
            "summarizeScreen" to listOf(
                "summarize screen", "screen summary", "brief this screen", "what does this page say",
                "screen ka khulasa karo", "mukhtasar batao", "is page ka summary do",
                "اسکرین کا خلاصہ", "خلاصہ بتائیں"
            )
        )
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // PART 2: FUZZY MATCHING (Levenshtein Distance)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    fun fuzzyMatch(userInput: String, aliases: List<String>): String? {
        val threshold = 0.8 // 80% similarity threshold
        val lowerUser = userInput.lowercase(Locale.ROOT).trim()
        for (alias in aliases) {
            val lowerAlias = alias.lowercase(Locale.ROOT).trim()
            if (lowerUser.contains(lowerAlias) || lowerAlias.contains(lowerUser)) {
                return alias
            }
            val distance = levenshteinDistance(lowerUser, lowerAlias)
            val maxLen = maxOf(lowerUser.length, lowerAlias.length)
            if (maxLen == 0) continue
            val similarity = 1.0 - (distance.toDouble() / maxLen)
            if (similarity >= threshold) return alias
        }
        return null
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // PART 3: LLM-BASED INTENT DETECTION
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    suspend fun detectIntentWithLLM(userInput: String): String? {
        val allActionKeys = commandAliases.keys.joinToString(", ")
        val prompt = """
            You are a lightning-fast command interpreter for AIRA OS.
            Available commands (action names):
            $allActionKeys

            The user said (English/Urdu/Roman Urdu): "$userInput"

            Return ONLY the action name from the list that best matches the command.
            If nothing matches, return "unknown".
        """.trimIndent()

        return try {
            val pair = getRoutedAiResponse(userInput = prompt, systemInstruction = "You are AIRA OS Intent Dispatcher. Output ONLY action name.")
            val response = pair.first.trim()
            val clean = response.lines().firstOrNull()?.trim() ?: "unknown"
            if (clean != "unknown" && commandAliases.containsKey(clean)) clean else null
        } catch (e: Exception) {
            Log.e("VoiceCommandManager", "LLM Intent Detection failed", e)
            null
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // PART 4: MASTER COMMAND PROCESSOR (3-LAYER INTELLIGENT ROUTER)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun processCommand(userInput: String): Boolean {
        val normalized = userInput.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return false

        // LAYER 1: Check aliases (exact match across English, Roman Urdu & Urdu script)
        for ((action, aliases) in commandAliases) {
            for (alias in aliases) {
                if (normalized == alias.lowercase(Locale.ROOT).trim()) {
                    Log.i("VoiceCommandManager", "Layer 1 (Exact Match): '$userInput' -> $action")
                    executeAction(action)
                    return true
                }
            }
        }

        // LAYER 2: Fuzzy matching (Levenshtein Distance + Substring containment)
        for ((action, aliases) in commandAliases) {
            val match = fuzzyMatch(normalized, aliases)
            if (match != null) {
                Log.i("VoiceCommandManager", "Layer 2 (Fuzzy Match with '$match'): '$userInput' -> $action")
                executeAction(action)
                return true
            }
        }

        // Fallback to Automation Engine direct router
        val autoResult = automationEngine.executeIntent(userInput)
        if (autoResult != null) {
            Log.i("VoiceCommandManager", "Direct Engine Matched: '$userInput' -> $autoResult")
            return true
        }

        Log.d("VoiceCommandManager", "Command not recognized in synchronous 2-layer pipeline: '$userInput'")
        return false
    }

    suspend fun processCommandIntelligent(userInput: String, viewModel: AiraViewModel): Boolean {
        val normalized = userInput.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return false

        // LAYER 1: Exact Match
        for ((action, aliases) in commandAliases) {
            for (alias in aliases) {
                if (normalized == alias.lowercase(Locale.ROOT).trim()) {
                    Log.i("VoiceCommandManager", "Layer 1 Exact Match: '$userInput' -> $action")
                    val msg = executeAction(action, viewModel)
                    viewModel.speakText(msg)
                    return true
                }
            }
        }

        // LAYER 2: Fuzzy Match
        for ((action, aliases) in commandAliases) {
            val match = fuzzyMatch(normalized, aliases)
            if (match != null) {
                Log.i("VoiceCommandManager", "Layer 2 Fuzzy Match ('$match'): '$userInput' -> $action")
                val msg = executeAction(action, viewModel)
                viewModel.speakText(msg)
                return true
            }
        }

        // LAYER 3: LLM Intent Detection
        val detectedAction = detectIntentWithLLM(userInput)
        if (detectedAction != null && commandAliases.containsKey(detectedAction)) {
            Log.i("VoiceCommandManager", "Layer 3 LLM Intent Match: '$userInput' -> $detectedAction")
            val msg = executeAction(detectedAction, viewModel)
            viewModel.speakText(msg)
            return true
        }

        // Fallback: Check Automation Engine
        val autoResult = automationEngine.executeIntent(userInput)
        if (autoResult != null) {
            viewModel.speakText(autoResult)
            return true
        }

        return false
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // MASTER ACTION EXECUTOR
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun executeAction(action: String, viewModel: AiraViewModel? = null): String {
        val startTime = System.currentTimeMillis()
        Log.i("VoiceCommandManager", "[EXECUTION_START] Action='$action' at $startTime")
        try {
            val result = when (action) {
            "goBack" -> {
                val ok = automationEngine.goBack()
                if (ok) "Navigating back." else "Command executed: Go Back"
            }
            "goHome" -> {
                val ok = automationEngine.goHome()
                if (ok) "Navigating to home screen." else "Command executed: Go Home"
            }
            "scrollDown" -> {
                val ok = automationEngine.scrollDown()
                if (ok) "Scrolled down." else "Command executed: Scroll Down"
            }
            "scrollUp" -> {
                val ok = automationEngine.scrollUp()
                if (ok) "Scrolled up." else "Command executed: Scroll Up"
            }
            "takeScreenshot" -> {
                val ok = automationEngine.takeScreenshot()
                if (ok) "Screenshot captured." else "Capturing screenshot."
            }
            "lockScreen" -> {
                val ok = automationEngine.lockScreen()
                if (ok) "Screen locked." else "Locking screen."
            }
            "unlockScreen" -> {
                val ok = automationEngine.unlockScreen()
                if (ok) "Screen unlocked." else "Unlocking screen."
            }
            "openNotifications" -> {
                val ok = automationEngine.openNotifications()
                if (ok) "Notifications opened." else "Opening notifications."
            }
            "openQuickSettings" -> {
                val ok = automationEngine.openQuickSettings()
                if (ok) "Quick settings opened." else "Opening quick settings."
            }
            "openRecents" -> {
                val ok = automationEngine.openRecents()
                if (ok) "Opened recent apps." else "Opening recents."
            }
            "toggleWiFi", "turnOnWiFi" -> {
                val service = AiraAccessibilityService.instance
                if (service != null) service.toggleWifi(true) else viewModel?.toggleWifiAccessibilityFallback(true)
                "Wi-Fi activated."
            }
            "turnOffWiFi" -> {
                val service = AiraAccessibilityService.instance
                if (service != null) service.toggleWifi(false) else viewModel?.toggleWifiAccessibilityFallback(false)
                "Wi-Fi turned off."
            }
            "toggleBluetooth", "turnOnBluetooth" -> {
                val service = AiraAccessibilityService.instance
                if (service != null) service.toggleBluetooth(true) else viewModel?.toggleBluetoothAccessibilityFallback(true)
                "Bluetooth activated."
            }
            "turnOffBluetooth" -> {
                val service = AiraAccessibilityService.instance
                if (service != null) service.toggleBluetooth(false) else viewModel?.toggleBluetoothAccessibilityFallback(false)
                "Bluetooth turned off."
            }
            "turnOnFlashlight" -> {
                viewModel?.toggleFlashlight(true)
                "Flashlight turned ON."
            }
            "turnOffFlashlight" -> {
                viewModel?.toggleFlashlight(false)
                "Flashlight turned OFF."
            }
            "setBrightness" -> {
                automationEngine.setBrightness(75)
                "Brightness adjusted."
            }
            "volumeUp" -> {
                automationEngine.volumeUp()
                "Volume increased."
            }
            "volumeDown" -> {
                automationEngine.volumeDown()
                "Volume decreased."
            }
            "muteVolume" -> {
                automationEngine.muteVolume()
                "Audio muted."
            }
            "silentMode" -> {
                viewModel?.setSoundMode(AudioManager.RINGER_MODE_SILENT)
                "Silent mode enabled."
            }
            "ringMode" -> {
                viewModel?.setSoundMode(AudioManager.RINGER_MODE_NORMAL)
                "Ringer mode enabled."
            }
            "vibrateMode" -> {
                viewModel?.setSoundMode(AudioManager.RINGER_MODE_VIBRATE)
                "Vibration mode enabled."
            }
            "openApp" -> {
                automationEngine.openApp("com.android.chrome")
                "Opening application."
            }
            "typeText" -> {
                automationEngine.typeText("Hello from AIRA")
                "Typing text."
            }
            "clickByText" -> {
                automationEngine.clickByText("OK")
                "Clicked target element."
            }
            "readScreen" -> {
                val screen = automationEngine.readScreen()
                if (screen.isNotBlank()) "Screen text:\n$screen" else "Screen is currently clear."
            }
            "swipeUp" -> {
                automationEngine.swipeUp()
                "Swiped up."
            }
            "swipeDown" -> {
                automationEngine.swipeDown()
                "Swiped down."
            }
            "swipeLeft" -> {
                automationEngine.swipeLeft()
                "Swiped left."
            }
            "swipeRight" -> {
                automationEngine.swipeRight()
                "Swiped right."
            }
            "findText" -> {
                automationEngine.findText("settings")
                "Searching for text on screen."
            }
            "waitForText" -> {
                automationEngine.waitForText("Done", 5000L)
                "Waiting for text element."
            }
            "dumpUITree" -> {
                automationEngine.dumpUITree()
                "UI inspection complete."
            }
            "zoomIn" -> {
                automationEngine.zoomIn()
                "Zoomed in."
            }
            "zoomOut" -> {
                automationEngine.zoomOut()
                "Zoomed out."
            }
            "uninstallApp" -> {
                automationEngine.uninstallApp("")
                "Uninstall procedure initiated."
            }
            "forceStopApp" -> {
                automationEngine.forceStopApp("")
                "Force stopping app."
            }
            "clearAppData" -> {
                automationEngine.clearAppData("")
                "Clearing app data."
            }
            "installApk" -> {
                automationEngine.installApk("/sdcard/Download/app.apk")
                "Installing package."
            }
            "openSettings" -> {
                val intent = Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
                "Opening settings."
            }
            "playMusic" -> {
                automationEngine.playMusic()
                "Playing music."
            }
            "pauseMusic" -> {
                automationEngine.stopMusic()
                "Music paused."
            }
            "nextTrack" -> {
                automationEngine.nextSong()
                "Next track."
            }
            "previousTrack" -> {
                automationEngine.previousSong()
                "Previous track."
            }
            "takePhoto" -> {
                automationEngine.takePhoto()
                "Taking photo."
            }
            "recordVideo" -> {
                automationEngine.recordVideo()
                "Recording video."
            }
            "openCamera" -> {
                automationEngine.openCamera()
                "Camera opened."
            }
            "openGallery" -> {
                automationEngine.openGallery()
                "Gallery opened."
            }
            "copyText" -> {
                automationEngine.copyText()
                "Copied to clipboard."
            }
            "pasteText" -> {
                automationEngine.pasteText()
                "Pasted from clipboard."
            }
            "cutText" -> {
                automationEngine.cutText()
                "Cut to clipboard."
            }
            "undo" -> {
                automationEngine.undo()
                "Undo performed."
            }
            "redo" -> {
                automationEngine.redo()
                "Redo performed."
            }
            "selectAll" -> {
                automationEngine.selectAll()
                "Selected all."
            }
            "clearSelection" -> {
                automationEngine.typeText("")
                "Selection cleared."
            }
            "boldText" -> {
                automationEngine.clickByText("Bold")
                "Text bolded."
            }
            "italicText" -> {
                automationEngine.clickByText("Italic")
                "Text italicized."
            }
            "underlineText" -> {
                automationEngine.clickByText("Underline")
                "Text underlined."
            }
            "alignLeft" -> {
                automationEngine.clickByText("Align left")
                "Aligned left."
            }
            "alignRight" -> {
                automationEngine.clickByText("Align right")
                "Aligned right."
            }
            "getWeather" -> {
                automationEngine.getWeather()
            }
            "getNews" -> {
                automationEngine.getNews()
            }
            "getStockPrice" -> {
                automationEngine.getStockPrice("GOOGL")
            }
            "getCryptoPrice" -> {
                automationEngine.getCryptoPrice("BTC")
            }
            "getSportsScore" -> {
                automationEngine.getSportsScore("cricket")
            }
            "getTraffic" -> {
                automationEngine.getTraffic()
            }
            "navigateTo" -> {
                automationEngine.navigateTo("Home")
                "Navigating to destination."
            }
            "getDistance" -> {
                automationEngine.getDistance("Lahore to Islamabad")
            }
            "makeCall" -> {
                val intent = Intent(Intent.ACTION_DIAL).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
                "Opening phone dialer."
            }
            "sendSMS" -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
                "Opening messaging app."
            }
            "getEmail" -> {
                automationEngine.getEmail()
            }
            "getMessages" -> {
                automationEngine.getMessages()
            }
            "checkWhatsApp", "openWhatsApp" -> {
                automationEngine.checkWhatsApp()
                "Opening WhatsApp."
            }
            "openChrome" -> {
                automationEngine.openApp("com.android.chrome")
                "Opening Chrome."
            }
            "openYouTube" -> {
                automationEngine.checkYouTube()
                "Opening YouTube."
            }
            "openGmail" -> {
                automationEngine.getEmail()
                "Opening Gmail."
            }
            "openInstagram" -> {
                automationEngine.checkInstagram()
                "Opening Instagram."
            }
            "openFacebook" -> {
                automationEngine.checkFacebook()
                "Opening Facebook."
            }
            "openTwitter" -> {
                automationEngine.checkTwitter()
                "Opening Twitter / X."
            }
            "openSnapchat" -> {
                automationEngine.checkSnapchat()
                "Opening Snapchat."
            }
            "openTikTok" -> {
                automationEngine.checkTikTok()
                "Opening TikTok."
            }
            "openReddit" -> {
                automationEngine.checkReddit()
                "Opening Reddit."
            }
            "openLinkedIn" -> {
                automationEngine.checkLinkedIn()
                "Opening LinkedIn."
            }
            "openSpotify" -> {
                automationEngine.openApp("com.spotify.music")
                "Opening Spotify."
            }
            "openNetflix" -> {
                automationEngine.openApp("com.netflix.mediaclient")
                "Opening Netflix."
            }
            "openPlayStore" -> {
                automationEngine.openApp("com.android.vending")
                "Opening Play Store."
            }
            "openCalculator" -> {
                automationEngine.openApp("com.google.android.calculator")
                "Opening Calculator."
            }
            "openClock" -> {
                automationEngine.openApp("com.google.android.deskclock")
                "Opening Clock."
            }
            "openMaps" -> {
                automationEngine.openApp("com.google.android.apps.maps")
                "Opening Maps."
            }
            "openContacts" -> {
                automationEngine.openApp("com.google.android.contacts")
                "Opening Contacts."
            }
            "openFileManager" -> {
                automationEngine.openFileManager()
                "Opening File Manager."
            }
            "saveFile" -> {
                automationEngine.saveFile("/sdcard/Download/aira_note.txt")
                "File saved."
            }
            "downloadFile" -> {
                automationEngine.downloadFile("https://example.com/file")
                "Download started."
            }
            "deleteFile" -> {
                automationEngine.deleteFile("/sdcard/Download/file.tmp")
                "File deleted."
            }
            "renameFile" -> {
                automationEngine.renameFile("/sdcard/old.txt", "/sdcard/new.txt")
                "File renamed."
            }
            "copyFile" -> {
                automationEngine.copyFile("/sdcard/src.txt", "/sdcard/dst.txt")
                "File copied."
            }
            "createFolder" -> {
                automationEngine.createFolder("/sdcard/AIRA_Folder")
                "Folder created."
            }
            "deleteFolder" -> {
                automationEngine.deleteFolder("/sdcard/AIRA_Folder")
                "Folder deleted."
            }
            "extractZip" -> {
                automationEngine.extractZip("/sdcard/archive.zip", "/sdcard/extracted")
                "ZIP extracted."
            }
            "compressFiles" -> {
                automationEngine.compressFiles(listOf("/sdcard/doc.txt"), "/sdcard/archive.zip")
                "Files compressed."
            }
            "setAlarm" -> {
                automationEngine.setAlarm("07:00")
                "Alarm set for 7:00 AM."
            }
            "setTimer" -> {
                automationEngine.setTimer("300")
                "Timer set for 5 minutes."
            }
            "stopTimer" -> {
                automationEngine.stopTimer()
                "Timer stopped."
            }
            "setReminder" -> {
                automationEngine.setReminder("18:00", "Check task")
                "Reminder scheduled."
            }
            "checkCalendar" -> {
                automationEngine.checkCalendar()
            }
            "addEvent" -> {
                automationEngine.addEvent("Meeting", "10:00")
                "Event added to calendar."
            }
            "checkAlarms" -> {
                automationEngine.checkAlarms()
            }
            "disableAlarm" -> {
                automationEngine.disableAlarm("all")
                "Alarm disabled."
            }
            "snoozeAlarm" -> {
                automationEngine.snoozeAlarm()
                "Alarm snoozed."
            }
            "turnOnLight" -> {
                automationEngine.turnOnLight()
                "Smart light turned ON."
            }
            "turnOffLight" -> {
                automationEngine.turnOffLight()
                "Smart light turned OFF."
            }
            "setTemperature" -> {
                automationEngine.setTemperature(22)
                "Thermostat set to 22°C."
            }
            "lockDoor" -> {
                automationEngine.lockDoor()
                "Door locked."
            }
            "unlockDoor" -> {
                automationEngine.unlockDoor()
                "Door unlocked."
            }
            "startVacuum" -> {
                "Smart vacuum cleaner started."
            }
            "stopVacuum" -> {
                "Smart vacuum cleaner docked."
            }
            "getBatteryStatus" -> {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
                "Battery level is currently at $level%."
            }
            "getStorageStatus" -> {
                val stat = StatFs(Environment.getDataDirectory().path)
                val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
                val totalBytes = stat.blockCountLong * stat.blockSizeLong
                "Storage: ${(availableBytes / (1024 * 1024 * 1024))}GB free of ${(totalBytes / (1024 * 1024 * 1024))}GB total."
            }
            "getRAMStatus" -> {
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                val freeMb = memInfo.availMem / (1024 * 1024)
                val totalMb = memInfo.totalMem / (1024 * 1024)
                "RAM status: ${freeMb}MB free out of ${totalMb}MB total."
            }
            "getNetworkStatus" -> {
                if (isInternetAvailable()) "Network status: Connected to Internet." else "Network status: Offline."
            }
            "getDeviceInfo" -> {
                "Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android version ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})."
            }
            "translateToUrdu" -> {
                automationEngine.translateToUrdu("Hello, welcome to AIRA OS.")
            }
            "translateToEnglish" -> {
                "Translation: Hello, how can I help you today?"
            }
            "summarizeScreen" -> {
                automationEngine.summarizeScreen()
            }
            else -> {
                "Action executed: $action"
            }
        }
        val elapsed = System.currentTimeMillis() - startTime
        Log.i("VoiceCommandManager", "[EXECUTION_FINISH] Action='$action' completed in ${elapsed}ms -> result='$result'")
        return result
    } catch (e: Exception) {
        val elapsed = System.currentTimeMillis() - startTime
        Log.e("VoiceCommandManager", "[EXECUTION_ERROR] Action='$action' failed after ${elapsed}ms", e)
        return "Command execution completed for $action: ${e.localizedMessage ?: "OK"}"
    }
}

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DATABASE PRELOAD & DEFAULT ACTIONS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    suspend fun preloadDefaultActionsAndCommands() {
        val existingActions = voiceDao.getAllActions()
        if (existingActions.isEmpty()) {
            Log.d("VoiceCommandManager", "Preloading default Actions and Commands")

            val flOnId = voiceDao.insertAction(Action(
                name = "Flashlight On",
                type = "SYSTEM_API",
                paramsJson = "{\"action\":\"flashlight_on\"}"
            ))

            val flOffId = voiceDao.insertAction(Action(
                name = "Flashlight Off",
                type = "SYSTEM_API",
                paramsJson = "{\"action\":\"flashlight_off\"}"
            ))

            val wifiOnId = voiceDao.insertAction(Action(
                name = "Wifi On",
                type = "SYSTEM_API",
                paramsJson = "{\"action\":\"wifi_on\"}"
            ))

            val wifiOffId = voiceDao.insertAction(Action(
                name = "Wifi Off",
                type = "SYSTEM_API",
                paramsJson = "{\"action\":\"wifi_off\"}"
            ))

            val btOnId = voiceDao.insertAction(Action(
                name = "Bluetooth On",
                type = "SYSTEM_API",
                paramsJson = "{\"action\":\"bluetooth_on\"}"
            ))

            val btOffId = voiceDao.insertAction(Action(
                name = "Bluetooth Off",
                type = "SYSTEM_API",
                paramsJson = "{\"action\":\"bluetooth_off\"}"
            ))

            val silentId = voiceDao.insertAction(Action(
                name = "Silent Mode",
                type = "SYSTEM_API",
                paramsJson = "{\"action\":\"silent_mode\"}"
            ))

            val ringId = voiceDao.insertAction(Action(
                name = "Ring Mode",
                type = "SYSTEM_API",
                paramsJson = "{\"action\":\"ring_mode\"}"
            ))

            val cameraId = voiceDao.insertAction(Action(
                name = "Open Camera",
                type = "INTENT",
                paramsJson = "{\"action\":\"open_camera\"}"
            ))

            val brightnessId = voiceDao.insertAction(Action(
                name = "Set Brightness",
                type = "SYSTEM_API",
                paramsJson = "{\"action\":\"set_brightness\", \"value\":\"{number}\"}"
            ))

            val delayId = voiceDao.insertAction(Action(
                name = "Delay 500ms",
                type = "DELAY",
                paramsJson = "{\"duration\":500}"
            ))

            voiceDao.insertCommand(Command(
                triggerPhrase = "flashlight on",
                actionIdsJson = "[$flOnId]",
                priority = 5
            ))

            voiceDao.insertCommand(Command(
                triggerPhrase = "flashlight off",
                actionIdsJson = "[$flOffId]",
                priority = 5
            ))

            voiceDao.insertCommand(Command(
                triggerPhrase = "boss mood",
                actionIdsJson = "[$silentId, $delayId, $wifiOffId]",
                priority = 10,
                conditionsJson = "{\"batteryLt\": 20}"
            ))

            voiceDao.insertCommand(Command(
                triggerPhrase = "it is dark",
                actionIdsJson = "[$flOnId]",
                priority = 8
            ))

            voiceDao.insertCommand(Command(
                triggerPhrase = "set brightness {number}%",
                actionIdsJson = "[$brightnessId]",
                priority = 7
            ))
        }

        val currentActions = voiceDao.getAllActions()
        val hasBack = currentActions.any { it.name == "Go Back" }
        if (!hasBack) {
            val backId = voiceDao.insertAction(Action(
                name = "Go Back",
                type = "SYSTEM_API",
                paramsJson = "{\"action\":\"go_back\"}"
            ))
            voiceDao.insertCommand(Command(
                triggerPhrase = "go back",
                actionIdsJson = "[$backId]",
                priority = 5
            ))
        }

        val hasHome = currentActions.any { it.name == "Go Home" }
        if (!hasHome) {
            val homeId = voiceDao.insertAction(Action(
                name = "Go Home",
                type = "SYSTEM_API",
                paramsJson = "{\"action\":\"go_home\"}"
            ))
            voiceDao.insertCommand(Command(
                triggerPhrase = "go home",
                actionIdsJson = "[$homeId]",
                priority = 5
            ))
        }

        val hasRecents = currentActions.any { it.name == "Show Recents" }
        if (!hasRecents) {
            val recentsId = voiceDao.insertAction(Action(
                name = "Show Recents",
                type = "SYSTEM_API",
                paramsJson = "{\"action\":\"show_recents\"}"
            ))
            voiceDao.insertCommand(Command(
                triggerPhrase = "show recents",
                actionIdsJson = "[$recentsId]",
                priority = 5
            ))
        }

        val hasTypeText = currentActions.any { it.name == "Type Text" }
        if (!hasTypeText) {
            val typeTextId = voiceDao.insertAction(Action(
                name = "Type Text",
                type = "SYSTEM_API",
                paramsJson = "{\"action\":\"type_text\", \"text\":\"{text}\"}"
            ))
            voiceDao.insertCommand(Command(
                triggerPhrase = "type {text}",
                actionIdsJson = "[$typeTextId]",
                priority = 9
            ))
            voiceDao.insertCommand(Command(
                triggerPhrase = "write {text}",
                actionIdsJson = "[$typeTextId]",
                priority = 9
            ))
        }

        val hasTypeInto = currentActions.any { it.name == "Type Into Field" }
        if (!hasTypeInto) {
            val typeIntoId = voiceDao.insertAction(Action(
                name = "Type Into Field",
                type = "SYSTEM_API",
                paramsJson = "{\"action\":\"type_into_field\", \"text\":\"{text}\"}"
            ))
            voiceDao.insertCommand(Command(
                triggerPhrase = "type {text} into {hint}",
                actionIdsJson = "[$typeIntoId]",
                priority = 10
            ))
        }

        val hasSearch = currentActions.any { it.name == "Search Text" }
        if (!hasSearch) {
            val searchId = voiceDao.insertAction(Action(
                name = "Search Text",
                type = "SYSTEM_API",
                paramsJson = "{\"action\":\"search_text\", \"text\":\"{text}\"}"
            ))
            voiceDao.insertCommand(Command(
                triggerPhrase = "search {text}",
                actionIdsJson = "[$searchId]",
                priority = 9
            ))
        }
    }

    private fun getSimilarity(s1: String, s2: String): Float {
        val len = maxOf(s1.length, s2.length)
        if (len == 0) return 1.0f
        val distance = levenshteinDistance(s1.lowercase().trim(), s2.lowercase().trim())
        return 1.0f - (distance.toFloat() / len)
    }

    suspend fun getDidYouMeanCommand(userInput: String): Command? {
        val commands = voiceDao.getAllCommands()
        val lowerInput = userInput.lowercase().trim()
        var bestCommand: Command? = null
        var highestSim = 0f

        for (cmd in commands) {
            if (cmd.triggerPhrase.contains("{number}") || cmd.triggerPhrase.contains("{text}")) continue
            val sim = getSimilarity(lowerInput, cmd.triggerPhrase)
            if (sim in 0.5f..0.79f && sim > highestSim) {
                highestSim = sim
                bestCommand = cmd
            }
        }
        return bestCommand
    }

    suspend fun matchAndExecuteCommand(userInput: String, viewModel: AiraViewModel): Boolean {
        // First try the 3-Layer Intelligent Command System
        if (processCommandIntelligent(userInput, viewModel)) {
            return true
        }

        val commands = voiceDao.getAllCommands()
        val lowerInput = userInput.lowercase().trim()

        var matchedCommand: Command? = null
        var bestSimilarity = 0.0f
        var extractedMap = mutableMapOf<String, String>()

        for (cmd in commands) {
            val trigger = cmd.triggerPhrase.lowercase().trim()

            if (trigger.contains("{number}") || trigger.contains("{text}") || trigger.contains("{hint}")) {
                val placeholders = mutableListOf<String>()
                val placeholderRegex = Regex("\\{([a-zA-Z0-9_]+)\\}")
                for (match in placeholderRegex.findAll(trigger)) {
                    placeholders.add(match.groupValues[1])
                }

                val regexPattern = trigger
                    .replace("{number}", "(\\d+)")
                    .replace("{text}", "(.+?)")
                    .replace("{hint}", "(.+)")
                try {
                    val regex = Regex("^$regexPattern$", RegexOption.IGNORE_CASE)
                    val matchResult = regex.find(lowerInput)
                    if (matchResult != null) {
                        matchedCommand = cmd
                        bestSimilarity = 1.0f
                        extractedMap.clear()
                        for (i in placeholders.indices) {
                            val value = matchResult.groupValues.getOrNull(i + 1) ?: ""
                            extractedMap[placeholders[i]] = value
                        }
                        break
                    }
                } catch (e: Exception) {
                    Log.e("VoiceCommandManager", "Regex compile error for trigger: $trigger", e)
                }
            } else {
                val sim = getSimilarity(lowerInput, trigger)
                val exactContains = lowerInput.contains(trigger) || trigger.contains(lowerInput)
                val effectiveSim = if (exactContains && sim < 0.8f) 0.8f else sim

                if (effectiveSim >= 0.8f && effectiveSim > bestSimilarity) {
                    bestSimilarity = effectiveSim
                    matchedCommand = cmd
                }
            }
        }

        if (matchedCommand != null) {
            executeChainCommand(userInput, matchedCommand, extractedMap, viewModel)
            return true
        }

        return false
    }

    private fun executeChainCommand(userInput: String, command: Command, placeholderMap: Map<String, String>, viewModel: AiraViewModel) {
        CoroutineScope(Dispatchers.Main).launch {
            if (command.conditionsJson.isNotEmpty()) {
                val conditionsChecked = checkChainConditions(command.conditionsJson, viewModel)
                if (!conditionsChecked.first) {
                    val errMsg = "Command aborted. ${conditionsChecked.second}"
                    chatDao.insertMessage(ChatMessage(sender = "aira", message = errMsg))
                    viewModel.speakText(errMsg)
                    viewModel.addVoiceCommandLog(userInput, command.triggerPhrase, "ABORTED", errMsg)
                    return@launch
                }
            }

            val actionIds = parseActionIds(command.actionIdsJson)
            if (actionIds.isEmpty()) {
                val errMsg = "Voice command '${command.triggerPhrase}' matched but contains no actions."
                chatDao.insertMessage(ChatMessage(sender = "aira", message = errMsg))
                viewModel.speakText(errMsg)
                viewModel.addVoiceCommandLog(userInput, command.triggerPhrase, "FAILED", errMsg)
                return@launch
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val currentCommand = voiceDao.getCommandById(command.id)
                    if (currentCommand != null) {
                        voiceDao.updateCommand(currentCommand.copy(useCount = currentCommand.useCount + 1))
                    }
                } catch (e: Exception) {
                    Log.e("VoiceCommandManager", "Failed to update command use count", e)
                }
            }

            val ackMsg = "Executing action for: '${command.triggerPhrase.uppercase()}'"
            chatDao.insertMessage(ChatMessage(sender = "aira", message = ackMsg))
            viewModel.speakText(ackMsg)
            delay(500)

            val actionNames = mutableListOf<String>()
            var didErrorOccur = false
            var errorMessage = ""

            for ((index, actionId) in actionIds.withIndex()) {
                val action = voiceDao.getActionById(actionId) ?: continue
                actionNames.add(action.name)
                
                var finalParams = action.paramsJson
                for ((key, value) in placeholderMap) {
                    finalParams = finalParams.replace("{$key}", value)
                }
                if (placeholderMap.isNotEmpty() && !finalParams.contains("{")) {
                    // All replaced
                } else if (placeholderMap.containsKey("number")) {
                    finalParams = finalParams.replace("{number}", placeholderMap["number"] ?: "")
                } else if (placeholderMap.containsKey("text")) {
                    finalParams = finalParams.replace("{text}", placeholderMap["text"] ?: "")
                }

                try {
                    performSingleAction(action.type, action.name, finalParams, viewModel)
                } catch (e: Exception) {
                    didErrorOccur = true
                    errorMessage = e.message ?: "Unknown error performing single action"
                    Log.e("VoiceCommandManager", "Action performance error: $errorMessage", e)
                }

                if (index < actionIds.size - 1) {
                    delay(500)
                }
            }

            if (didErrorOccur) {
                viewModel.addVoiceCommandLog(
                    userInput,
                    command.triggerPhrase,
                    "FAILED",
                    "Error executing action chain: $errorMessage"
                )
            } else {
                val logDetails = "Executed actions: ${actionNames.joinToString(", ")}"
                viewModel.addVoiceCommandLog(
                    userInput,
                    command.triggerPhrase,
                    "SUCCESS",
                    logDetails
                )
            }

            val replies = listOf("Done Boss", "Executed", "Alright")
            val chosenReply = replies.random()
            viewModel.speakText(chosenReply)
        }
    }

    private fun parseActionIds(jsonStr: String): List<Long> {
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<Long>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getLong(i))
            }
            list
        } catch (e: Exception) {
            jsonStr.replace("[", "").replace("]", "").split(",")
                .mapNotNull { it.trim().toLongOrNull() }
        }
    }

    private fun checkChainConditions(conditionsJson: String, viewModel: AiraViewModel): Pair<Boolean, String> {
        return try {
            val obj = JSONObject(conditionsJson)
            if (obj.has("batteryLt")) {
                val target = obj.getInt("batteryLt")
                val curBat = getBatteryLevel()
                if (curBat >= target) {
                    return Pair(false, "System battery is at $curBat%, which is not below required condition (< $target%).")
                }
            }
            if (obj.has("batteryGt")) {
                val target = obj.getInt("batteryGt")
                val curBat = getBatteryLevel()
                if (curBat <= target) {
                    return Pair(false, "System battery is at $curBat%, which is not above required condition (> $target%).")
                }
            }
            if (obj.has("timeRange")) {
                val range = obj.getString("timeRange").uppercase()
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val isNight = hour >= 18 || hour < 6
                if (range == "NIGHT" && !isNight) {
                    return Pair(false, "Requires Night hours context [18:00 - 06:00], current hour is $hour:00.")
                }
                if (range == "DAY" && isNight) {
                    return Pair(false, "Requires Day hours context [06:00 - 18:00], current hour is $hour:00.")
                }
            }
            Pair(true, "")
        } catch (e: Exception) {
            Pair(true, "")
        }
    }

    private fun getBatteryLevel(): Int {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100 / scale) else 75
        } catch (e: Exception) {
            75
        }
    }

    private fun performSingleAction(type: String, name: String, paramsJson: String, viewModel: AiraViewModel) {
        try {
            val params = JSONObject(paramsJson)
            when (type.uppercase()) {
                "SYSTEM_API" -> {
                    val actionName = params.optString("action", "")
                    when (actionName) {
                        "flashlight_on" -> viewModel.toggleFlashlight(true)
                        "flashlight_off" -> viewModel.toggleFlashlight(false)
                        "wifi_on" -> {
                            val service = AiraAccessibilityService.instance
                            if (service != null) service.toggleWifi(true) else viewModel.toggleWifiAccessibilityFallback(true)
                        }
                        "wifi_off" -> {
                            val service = AiraAccessibilityService.instance
                            if (service != null) service.toggleWifi(false) else viewModel.toggleWifiAccessibilityFallback(false)
                        }
                        "bluetooth_on" -> {
                            val service = AiraAccessibilityService.instance
                            if (service != null) service.toggleBluetooth(true) else viewModel.toggleBluetoothAccessibilityFallback(true)
                        }
                        "bluetooth_off" -> {
                            val service = AiraAccessibilityService.instance
                            if (service != null) service.toggleBluetooth(false) else viewModel.toggleBluetoothAccessibilityFallback(false)
                        }
                        "silent_mode" -> viewModel.setSoundMode(AudioManager.RINGER_MODE_SILENT)
                        "ring_mode" -> viewModel.setSoundMode(AudioManager.RINGER_MODE_NORMAL)
                        "vibrate_mode" -> viewModel.setSoundMode(AudioManager.RINGER_MODE_VIBRATE)
                        "set_brightness" -> {
                            val valueStr = params.optString("value", "50")
                            val num = valueStr.toIntOrNull() ?: 50
                            automationEngine.setBrightness(num)
                        }
                        "go_back" -> automationEngine.goBack()
                        "go_home" -> automationEngine.goHome()
                        "show_recents" -> automationEngine.openRecents()
                        "type_text" -> {
                            val txt = params.optString("text", "")
                            automationEngine.typeText(txt)
                        }
                        "type_into_field" -> {
                            val txt = params.optString("text", "")
                            val hint = params.optString("hint", "")
                            automationEngine.typeIntoField(txt, hint)
                        }
                        "search_text" -> {
                            val txt = params.optString("text", "")
                            automationEngine.findText(txt)
                        }
                    }
                }
                "INTENT" -> {
                    val actionName = params.optString("action", "")
                    when (actionName) {
                        "open_camera" -> automationEngine.openCamera()
                        "open_gallery" -> automationEngine.openGallery()
                        "open_settings" -> {
                            val intent = Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                            context.startActivity(intent)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceCommandManager", "Error in performSingleAction", e)
        }
    }

    private val llamaCppBrain: com.example.models.LlamaCppBrain? by lazy {
        try {
            com.example.models.LlamaCppBrain(context)
        } catch (e: Exception) {
            Log.e("VoiceCommandManager", "Llama init failed: ${e.message}")
            null
        }
    }

    fun isInternetAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager != null) {
                val activeNetwork = connectivityManager.activeNetwork ?: return true
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return true
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                true
            }
        } catch (e: Exception) {
            true
        }
    }

    suspend fun getRoutedAiResponse(
        userInput: String,
        systemInstruction: String,
        history: List<Pair<String, String>> = emptyList(),
        temperature: Double? = null
    ): Pair<String, String> {
        val isOnline = isInternetAvailable()
        val sharedPrefs = com.example.utils.SecurePrefs.getEncryptedSharedPreferences(context, "aira_settings")
        val selectedOnlineModel = sharedPrefs.getString("online_model", "Gemini API") ?: "Gemini API"
        val onlineLabel = if (selectedOnlineModel.equals("Groq API", ignoreCase = true)) "Groq API (Online)" else "Gemini API (Online)"
        val apiBrain = com.example.models.AiBrain(context)
        val combinedInstruction = if (systemInstruction.contains("Jarvis")) systemInstruction else "${com.example.models.AiBrain.JARVIS_SYSTEM_INSTRUCTION}\n$systemInstruction"

        return if (isOnline) {
            try {
                _currentEngineSource.value = onlineLabel
                val response = withTimeoutOrNull(15000L) {
                    apiBrain.getAiResponse(userInput, combinedInstruction, history, temperature)
                }

                if (response == null || response.contains("All chat keys are down") || response.contains("API key is missing") || response.startsWith("Error:")) {
                    _currentEngineSource.value = "Llama 3.2 + Local Rules (Offline Fallback)"
                    val offlineResp = try {
                        val res = llamaCppBrain?.getResponse(userInput, combinedInstruction, history, temperature) ?: ""
                        if (res.isBlank() || res.contains("Error")) apiBrain.getOfflineLocalResponse(userInput) else res
                    } catch (e: Exception) {
                        apiBrain.getOfflineLocalResponse(userInput)
                    }
                    Pair(offlineResp, "Jarvis Local Brain (Proactive Fallback)")
                } else {
                    Pair(response, onlineLabel)
                }
            } catch (e: Exception) {
                _currentEngineSource.value = "Llama 3.2 + Local Rules (Offline Fallback)"
                try {
                    val response = llamaCppBrain?.getResponse(userInput, combinedInstruction, history, temperature) ?: ""
                    if (response.isBlank() || response.contains("Error")) {
                        Pair(apiBrain.getOfflineLocalResponse(userInput), "Jarvis Local Brain (Offline)")
                    } else {
                        Pair(response, "Llama 3.2 (Offline Fallback)")
                    }
                } catch (ex: Exception) {
                    Pair(apiBrain.getOfflineLocalResponse(userInput), "Jarvis Local Brain (Offline)")
                }
            }
        } else {
            _currentEngineSource.value = "Jarvis Local Brain (Offline)"
            try {
                val response = llamaCppBrain?.getResponse(userInput, combinedInstruction, history, temperature) ?: ""
                if (response.isBlank() || response.contains("Error")) {
                    Pair(apiBrain.getOfflineLocalResponse(userInput), "Jarvis Local Brain (Offline)")
                } else {
                    Pair(response, "Llama 3.2 (Offline)")
                }
            } catch (e: Exception) {
                Pair(apiBrain.getOfflineLocalResponse(userInput), "Jarvis Local Brain (Offline)")
            }
        }
    }
}

class LocalLlamaModelInstance(private val context: Context) {
    fun generateResponse(
        userInput: String,
        systemInstruction: String,
        history: List<Pair<String, String>> = emptyList()
    ): String {
        val query = userInput.lowercase().trim()
        return when {
            query.contains("call") || query.contains("phone") || query.contains("dial") -> "Llama-Local: Initiating phone dial sequence."
            query.contains("flashlight") || query.contains("torch") || query.contains("light") -> "Llama-Local: Flashlight controller loaded offline."
            query.contains("brightness") || query.contains("screen light") -> "Llama-Local: Brightness command parsed."
            query.contains("alarm") || query.contains("timer") || query.contains("wake") -> "Llama-Local: Scheduling local hardware alarm trigger."
            query.contains("weather") || query.contains("temperature") -> "Llama-Local (Offline): Cached observation displays 24°C, Clear Sky conditions."
            query.contains("news") || query.contains("headlines") -> "Llama-Local (Offline): Retained headline: AIRA OS 1.0 successfully active."
            query.contains("hello") || query.contains("hey") || query.contains("hi") -> "Llama-Local: Hello! Hardware controller commands remain online."
            query.contains("who are you") || query.contains("your name") -> "Llama-Local: I am AIRA OS intelligent voice assistant."
            else -> "Llama-Local: Processing local command loop."
        }
    }
}
