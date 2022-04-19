package top.zibin.luban.example

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import top.zibin.luban.kt.Luban
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView

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

        textView = findViewById(R.id.main_info)

        findViewById<View>(R.id.main_select)?.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(intent, 10086)
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            Thread {
                val file = data?.data?.let {
                    Luban.with(this).compress {
                        input(uri = it, context = this@MainActivity)
                        output(dir = File(cacheDir, "image_cache"))
                        bilinear(true)
                        ignoreBy(20)
//                        quality(70)
//                        maxSize(60)
                        rename { "test-$it" }
//                        keepConfig(true)
                    }
                }
                runOnUiThread {
                    textView.text = file.toString()
                }
            }.start()
        }
    }

}