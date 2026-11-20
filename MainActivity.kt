package com.miso.cat
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.webkit.*
import org.json.*
import java.io.File

class MainActivity : Activity() {
  override fun onCreate(b: Bundle?) {
    super.onCreate(b)
    val web = WebView(this)
    web.settings.javaScriptEnabled = true
    web.settings.domStorageEnabled = true
    web.addJavascriptInterface(Bridge(), "Android")
    web.loadUrl("file:///android_asset/index.html")
    setContentView(web)
  }
  inner class Bridge {
    private val root: File = Environment.getExternalStorageDirectory()
    @JavascriptInterface fun hasAccess() = Environment.isExternalStorageManager()
    @JavascriptInterface fun requestAccess() {
      startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
    }
    @JavascriptInterface fun scan(): String {
      val out = JSONArray()
      fun add(f: File, why: String) { out.put(JSONObject().put("path", f.absolutePath).put("size", f.length()).put("why", why)) }
      cacheDir.walkTopDown().filter { it.isFile }.forEach { add(it, "App cache") }
      File(root, "DCIM/.thumbnails").walkTopDown().filter { it.isFile }.forEach { add(it, "Thumbnail cache") }
      File(root, "Download").walkTopDown().filter { it.isFile }.forEach {
        val n = it.name.lowercase()
        if (n.endsWith(".apk")) add(it, "Old installer")
        else if (n.endsWith(".tmp") || n.endsWith(".log") || n.endsWith(".crdownload")) add(it, "Temp file")
        else if (it.length() > 100L * 1024 * 1024) add(it, "Large file")
      }
      return out.toString()
    }
    @JavascriptInterface fun delete(json: String): Long {
      var freed = 0L
      val arr = JSONArray(json)
      for (i in 0 until arr.length()) {
        val f = File(arr.getString(i))
        val ok = f.canonicalPath.startsWith(root.canonicalPath) || f.canonicalPath.startsWith(cacheDir.canonicalPath)
        if (ok && f.isFile) { val s = f.length(); if (f.delete()) freed += s }
      }
      return freed
    }
  }
}
