# LogAssist
A log assist to help catch the log during system startup as many regular log may missing during startup.


/*
 * This class is used to cache log inside memory and print it out at the time when we want to check it.
 *
 * Enable the function:
 *
 * By default it is disabled, to enabled it, create an empty file named as "logassist_enabled" under
 * the specific path defined by "SWITCHER_FILE".
 *
 * Print:
 * To trigger the print, just send a broadcast from adb console using command :
 * 
 * am broadcast -a action.com.bely.logassist.print
 *
 * by default it will print the cached log immediately, if you have another adb console with logcat running
 * you will see it using :
 * 
 * logcat -s AudioAppLogAssist
 * 
 * But if you only have one adb console, you may miss it after you send the broadcast, then you can choose to
 * delay the print, to delay it, sending broadcast with a delayed integer value , like following:
 * 
 * am broadcast -a action.com.bely.logassist.print --ei delay 5000
 * 
 * 5000 above means delay 5 seconds to print, then you will chance to run logcat before it print.
 *
 * Control:
 *
 * Also you can pause/resume the log, or clear the cache buffer using following intent:
 *
 * action.com.bely.logassist.control.pause
 * action.com.bely.logassist.control.resume
 * action.com.bely.logassist.control.clear
 *
 */
