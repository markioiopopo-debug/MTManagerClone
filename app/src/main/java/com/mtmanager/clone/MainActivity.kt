package com.mtmanager.clone

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

private lateinit var info: TextView

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val layout = LinearLayout(this)
    layout.orientation = LinearLayout.VERTICAL
    layout.setPadding(24, 32, 24, 24)
    layout.setBackgroundColor(0xFF101820.toInt())

    val title = TextView(this)
    title.text = "MT Manager"
    title.textSize = 28f
    title.setTextColor(0xFFFFFFFF.toInt())
    title.gravity = Gravity.CENTER
    layout.addView(title)

    val subtitle = TextView(this)
    subtitle.text = "ตัวจัดการไฟล์ส่วนตัว"
    subtitle.textSize = 16f
    subtitle.setTextColor(0xFFBBBBBB.toInt())
    subtitle.gravity = Gravity.CENTER
    layout.addView(subtitle)

    val button = Button(this)
    button.text = "📂 เลือกไฟล์จากมือถือ"
    layout.addView(button)

    info = TextView(this)
    info.text = "ยังไม่ได้เลือกไฟล์"
    info.textSize = 16f
    info.setTextColor(0xFFFFFFFF.toInt())
    info.setPadding(0, 24, 0, 0)
    layout.addView(info)

    button.setOnClickListener {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, 100)
    }

    setContentView(layout)
}

@Deprecated("Deprecated in Android API")
override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == 100 && resultCode == RESULT_OK) {
        val uri: Uri = data?.data ?: return
        info.text = "ไฟล์ที่เลือก:\n${uri.lastPathSegment}"
        Toast.makeText(
            this,
            "เลือกไฟล์สำเร็จ",
            Toast.LENGTH_SHORT
        ).show()
    }
}

}
