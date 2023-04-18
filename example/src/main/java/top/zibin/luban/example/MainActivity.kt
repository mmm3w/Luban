package top.zibin.luban.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import top.zibin.luban.kt.Luban
import java.io.ByteArrayOutputStream
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var inputType: TextView
    private lateinit var inputContent: TextView
    private lateinit var inputImage: ImageView

    private lateinit var outputType: TextView
    private lateinit var outputContent: TextView
    private lateinit var outputImage: ImageView

    companion object {
        const val INPUT_STREAM = "INPUT_STREAM"
        const val INPUT_URI = "INPUT_URI"
        const val INPUT_BASE64 = "INPUT_BASE64"
        const val INPUT_BITMAP = "INPUT_BITMAP"

        const val OUTPUT_STREAM = "OUTPUT_STREAM"
        const val OUTPUT_FILE = "OUTPUT_FILE"
        const val OUTPUT_BASE64 = "OUTPUT_BASE64"
        const val OUTPUT_BITMAP = "OUTPUT_BITMAP"
    }


    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            10087
        )

        findViewById<View>(R.id.main_select)?.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(intent, 10086)
        }

        inputType = findViewById<TextView>(R.id.input_type).apply {
            setOnClickListener { ttt ->
                ttt.showPopupMenu(this@MainActivity, R.menu.input) {
                    when (it) {
                        R.id.input_stream -> (ttt as TextView).text = INPUT_STREAM
                        R.id.input_uri -> (ttt as TextView).text = INPUT_URI
                        R.id.input_base64 -> (ttt as TextView).text = INPUT_BASE64
                        R.id.input_bitmap -> (ttt as TextView).text = INPUT_BITMAP
                    }

                }
            }
        }
        inputContent = findViewById(R.id.input_content)
        inputImage = findViewById(R.id.input_image)
        outputType = findViewById<TextView>(R.id.output_type).apply {
            setOnClickListener { ttt ->
                ttt.showPopupMenu(this@MainActivity, R.menu.output) {
                    when (it) {
                        R.id.output_stream -> (ttt as TextView).text = OUTPUT_STREAM
                        R.id.output_file -> (ttt as TextView).text = OUTPUT_FILE
                        R.id.output_base64 -> (ttt as TextView).text = OUTPUT_BASE64
                        R.id.output_bitmap -> (ttt as TextView).text = OUTPUT_BITMAP
                    }
                }
            }
        }
        outputContent = findViewById(R.id.output_content)
        outputImage = findViewById(R.id.output_image)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            Thread { data?.data?.let { compress(it) } }.start()
        }
    }


    fun View.showPopupMenu(context: Context, @MenuRes menu: Int, click: (Int) -> Unit) {
        val popupMenu = PopupMenu(context, this)
        popupMenu.menuInflater.inflate(menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener {
            it?.itemId?.apply(click)
            true
        }
        popupMenu.show()
    }

    private fun compress(uri: Uri) {
        val luban = Luban.with(this)
        when (inputType.text) {
            INPUT_STREAM -> {
                val inputStream = contentResolver.openInputStream(uri)
                luban.input(inputStream = inputStream)
                runOnUiThread { inputContent.text = inputStream.toString() }
            }
            INPUT_URI -> {
                luban.input(uri = uri, context = this)
                runOnUiThread { inputContent.text = uri.toString() }
            }
            INPUT_BASE64 -> {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    ByteArrayOutputStream().use { baos ->
                        inputStream.copyTo(baos)
                        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_PADDING)
                        luban.input(base64 = b64)
                        runOnUiThread { inputContent.text = b64 }
                    }
                }
            }
            INPUT_BITMAP -> {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    luban.input(bitmap = bitmap)
                    runOnUiThread { inputImage.setImageBitmap(bitmap) }
                }
            }
        }


        luban.quality(80)
        luban.bilinear(true)
        luban.baseSize(1920)
//        luban.keepResolution(true)

        when (outputType.text) {
            OUTPUT_STREAM -> {
                val baos = ByteArrayOutputStream()
                luban.output(outputStream = baos)
                luban.get()
                runOnUiThread { outputContent.text = baos.toString() }
            }
            OUTPUT_FILE -> {
                val file = luban.getFile()
                runOnUiThread { outputContent.text = file.absolutePath }
            }
            OUTPUT_BASE64 -> {
                val base64 = luban.getBase64()
                runOnUiThread { outputContent.text = base64 }
            }
            OUTPUT_BITMAP -> {
                val bitmap = luban.getBitmap()
                runOnUiThread { outputImage.setImageBitmap(bitmap) }
            }
        }


    }
}