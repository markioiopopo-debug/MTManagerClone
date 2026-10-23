package com.mtmanager.clone

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var fileList: LinearLayout
    private lateinit var status: TextView

    private val folderRequest = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(16, 24, 16, 16)
        root.setBackgroundColor(0xFF101820.toInt())

        val title = TextView(this)
        title.text = "📂 MT Manager"
        title.textSize = 28f
        title.gravity = Gravity.CENTER
        title.setTextColor(0xFFFFFFFF.toInt())
        root.addView(title)

        val subtitle = TextView(this)
        subtitle.text = "ตัวจัดการไฟล์ส่วนตัว"
        subtitle.textSize = 15f
        subtitle.gravity = Gravity.CENTER
        subtitle.setTextColor(0xFFBBBBBB.toInt())
        root.addView(subtitle)

        val chooseButton = Button(this)
        chooseButton.text = "📁 เลือกโฟลเดอร์ในมือถือ"
        root.addView(chooseButton)

        status = TextView(this)
        status.text = "กรุณาเลือกโฟลเดอร์เพื่อดูไฟล์"
        status.textSize = 14f
        status.setTextColor(0xFFFFFFFF.toInt())
        status.setPadding(4, 12, 4, 12)
        root.addView(status)

        val scroll = ScrollView(this)
        fileList = LinearLayout(this)
        fileList.orientation = LinearLayout.VERTICAL
        scroll.addView(fileList)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        chooseButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, folderRequest)
        }

        setContentView(root)
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == folderRequest && resultCode == RESULT_OK) {
            val uri = data?.data ?: return

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            showFiles(uri)
        }
    }

    private fun showFiles(treeUri: Uri) {
        fileList.removeAllViews()

        try {
            val treeId =
                DocumentsContract.getTreeDocumentId(treeUri)

            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    treeId
                )

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            )

            val rows = mutableListOf<Triple<String, String, String>>()

            contentResolver.query(
                childrenUri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->

                while (cursor.moveToNext()) {
                    val name = cursor.getString(1) ?: "ไม่มีชื่อ"
                    val mime = cursor.getString(2) ?: ""
                    val size = if (cursor.isNull(3)) {
                        0L
                    } else {
                        cursor.getLong(3)
                    }

                    val isFolder =
                        mime == DocumentsContract.Document.MIME_TYPE_DIR

                    val display = if (isFolder) {
                        "📁  $name"
                    } else {
                        "📄  $name"
                    }

                    val detail = if (isFolder) {
                        "โฟลเดอร์"
                    } else {
                        formatSize(size)
                    }

                    rows.add(Triple(display, detail, name))
                }
            }

            rows.sortBy { it.third.lowercase(Locale.ROOT) }

            status.text = "พบ ${rows.size} รายการ"

            if (rows.isEmpty()) {
                addMessage("โฟลเดอร์นี้ยังไม่มีไฟล์")
            }

            for (row in rows) {
                val item = TextView(this)
                item.text = "${row.first}\n    ${row.second}"
                item.textSize = 16f
                item.setTextColor(0xFFFFFFFF.toInt())
                item.setPadding(12, 16, 12, 16)
                item.setBackgroundColor(0xFF1E293B.toInt())

                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 4, 0, 4)

                fileList.addView(item, params)
            }

        } catch (e: Exception) {
            status.text = "ไม่สามารถอ่านโฟลเดอร์นี้ได้"
            addMessage("ลองเลือกโฟลเดอร์อื่นนะ")
        }
    }

    private fun addMessage(message: String) {
        val text = TextView(this)
        text.text = message
        text.textSize = 16f
        text.setTextColor(0xFFBBBBBB.toInt())
        text.setPadding(12, 20, 12, 20)
        fileList.addView(text)
    }

    private fun formatSize(size: Long): String {
        if (size < 1024) return "$size B"

        if (size < 1024 * 1024) {
            return String.format(
                Locale.US,
                "%.1f KB",
                size / 1024.0
            )
        }

        return String.format(
            Locale.US,
            "%.2f MB",
            size / (1024.0 * 1024.0)
        )
    }
}
