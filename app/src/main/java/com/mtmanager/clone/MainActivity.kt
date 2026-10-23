
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

    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private lateinit var backButton: Button

    private var treeUri: Uri? = null
    private var currentId: String? = null
    private var viewingFile = false

    private val folderRequest = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(16, 20, 16, 16)
        root.setBackgroundColor(0xFF101820.toInt())

        val title = TextView(this)
        title.text = "📂 MT Manager"
        title.textSize = 26f
        title.gravity = Gravity.CENTER
        title.setTextColor(-1)
        root.addView(title)

        backButton = Button(this)
        backButton.text = "⬅️ ย้อนกลับ"
        root.addView(backButton)

        val chooseButton = Button(this)
        chooseButton.text = "📁 เลือกโฟลเดอร์"
        root.addView(chooseButton)

        status = TextView(this)
        status.text = "เลือกโฟลเดอร์เพื่อเริ่มต้น"
        status.textSize = 15f
        status.setTextColor(-1)
        status.setPadding(4, 12, 4, 12)
        root.addView(status)

        val scroll = ScrollView(this)
        list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        scroll.addView(list)

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

        backButton.setOnClickListener {
            if (viewingFile) {
                viewingFile = false
                showFiles()
            } else {
                goBack()
            }
        }

        setContentView(root)
        updateBackButton()
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

            treeUri = uri
            currentId = DocumentsContract.getTreeDocumentId(uri)
            viewingFile = false
            showFiles()
        }
    }

    private fun showFiles() {
        list.removeAllViews()
        viewingFile = false

        val tree = treeUri ?: return
        val folderId = currentId ?: return

        try {
            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    tree, folderId
                )

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            )

            val rows = mutableListOf<Array<String>>()

            contentResolver.query(
                childrenUri, projection, null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0) ?: continue
                    val name = cursor.getString(1) ?: "ไม่มีชื่อ"
                    val mime = cursor.getString(2) ?: ""
                    val size = if (cursor.isNull(3)) {
                        0L
                    } else {
                        cursor.getLong(3)
                    }

                    rows.add(
                        arrayOf(id, name, mime, size.toString())
                    )
                }
            }

            rows.sortBy { it[1].lowercase(Locale.ROOT) }
            status.text = "พบ ${rows.size} รายการ"

            if (rows.isEmpty()) {
                addMessage("โฟลเดอร์นี้ว่าง")
            }

            for (row in rows) {
                val id = row[0]
                val name = row[1]
                val mime = row[2]
                val size = row[3].toLongOrNull() ?: 0L

                val isFolder =
                    mime == DocumentsContract.Document.MIME_TYPE_DIR

                val extension = if (isFolder) {
                    ""
                } else {
                    name.substringAfterLast('.', "")
                        .takeIf { name.contains(".") && it.isNotBlank() }
                        ?: "ไม่มีนามสกุล"
                }

                val typeText = when {
                    isFolder -> "โฟลเดอร์"
                    extension == "ไม่มีนามสกุล" -> "ไฟล์ไม่มีนามสกุล"
                    else -> "นามสกุล: .$extension"
                }

                val item = TextView(this)
                item.text =
                    (if (isFolder) "📁  " else "📄  ") +
                    name + "\n    " +
                    if (isFolder) typeText
                    else "$typeText • ${formatSize(size)}"

                item.textSize = 16f
                item.setTextColor(-1)
                item.setPadding(12, 16, 12, 16)
                item.setBackgroundColor(0xFF1E293B.toInt())
                item.isClickable = true
                item.isFocusable = true

                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 4, 0, 4)
                list.addView(item, params)

                item.setOnClickListener {
                    if (isFolder) {
                        currentId = id
                        showFiles()
                    } else {
                        openFile(id, name, mime)
                    }
                }
            }

        } catch (_: Exception) {
            status.text = "อ่านโฟลเดอร์ไม่สำเร็จ"
            addMessage("ลองเลือกโฟลเดอร์อื่น")
        }

        updateBackButton()
    }

    private fun openFile(id: String, name: String, mime: String) {
        val tree = treeUri ?: return
        val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)

        val extension = name.substringAfterLast('.', "")
            .lowercase(Locale.ROOT)

        val textExtensions = setOf(
            "txt", "html", "htm", "xml", "json", "kt",
            "java", "gradle", "md", "log", "yml", "yaml",
            "css", "js", "csv", "properties", "sh", "py"
        )

        val isText = mime.startsWith("text/") ||
            extension in textExtensions ||
            mime == "application/json" ||
            mime == "application/xml"

        if (!isText) {
            Toast.makeText(
                this,
                "ไฟล์ชนิดนี้ยังไม่มีตัวอ่านในแอป",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            val input = contentResolver.openInputStream(uri)
                ?: throw Exception("เปิดไฟล์ไม่ได้")

            val bytes = input.use {
                it.readNBytes(1024 * 1024 + 1)
            }

            if (bytes.size > 1024 * 1024) {
                Toast.makeText(
                    this,
                    "ไฟล์ใหญ่เกิน 1 MB",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val text = String(bytes, Charsets.UTF_8)
            showText(name, text)

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "เปิดไฟล์ไม่สำเร็จ",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showText(name: String, text: String) {
        list.removeAllViews()
        viewingFile = true

        val heading = TextView(this)
        heading.text = "📄 $name"
        heading.textSize = 18f
        heading.setTextColor(-1)
        heading.setPadding(8, 8, 8, 12)
        list.addView(heading)

        val content = TextView(this)
        content.text = text
        content.textSize = 14f
        content.setTextColor(0xFFE2E8F0.toInt())
        content.setTextIsSelectable(true)
        content.setPadding(8, 8, 8, 20)
        list.addView(content)

        status.text = "กำลังอ่าน: $name"
        updateBackButton()
    }

    private fun goBack() {
        val tree = treeUri ?: return
        val id = currentId ?: return
        val rootId = DocumentsContract.getTreeDocumentId(tree)

        if (id == rootId) {
            status.text = "อยู่ที่โฟลเดอร์เริ่มต้นแล้ว"
            return
        }

        try {
            val uri = DocumentsContract.buildDocumentUriUsingTree(
                tree, id
            )
            val path = DocumentsContract.findDocumentPath(
                contentResolver, uri
            )?.path

            val parts = path?.split("/")?.filter { it.isNotEmpty() }

            currentId = if (parts != null && parts.size > 1) {
                parts.dropLast(1).joinToString("/")
            } else {
                rootId
            }

            showFiles()
        } catch (_: Exception) {
            currentId = rootId
            showFiles()
        }
    }

    private fun updateBackButton() {
        backButton.text = if (viewingFile) {
            "⬅️ กลับไปยังรายการไฟล์"
        } else {
            "⬅️ ย้อนกลับ"
        }
    }

    private fun addMessage(message: String) {
        val text = TextView(this)
        text.text = message
        text.textSize = 16f
        text.setTextColor(0xFFBBBBBB.toInt())
        text.setPadding(12, 20, 12, 20)
        list.addView(text)
    }

    private fun formatSize(size: Long): String {
        if (size < 1024) return "$size B"

        if (size < 1024 * 1024) {
            return String.format(
                Locale.US, "%.1f KB", size / 1024.0
            )
        }

        return String.format(
            Locale.US, "%.2f MB",
            size / (1024.0 * 1024.0)
        )
    }
}
