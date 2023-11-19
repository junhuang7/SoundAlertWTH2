package com.maple.audiometry.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import com.maple.audiometry.R
import com.maple.audiometry.ui.base.BaseFragmentActivity
import com.maple.audiometry.ui.detection.DetectionActivity
import com.maple.audiometry.ui.noise.NoiseCheckActivity
import com.maple.audiometry.utils.T
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseFragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        bt_noise.setOnClickListener { toCheckNoise() }
        bt_voice.setOnClickListener { toCheckEar() }
        bt_yamnet.setOnClickListener { launchYamnetApp() }
    }

    // 去检测噪音
    private fun toCheckNoise() {
        val intent = Intent(this, NoiseCheckActivity::class.java)
        startActivity(intent)
    }

    // 去检查耳朵
    private fun toCheckEar() {
        val intent = Intent(this, DetectionActivity::class.java)
        startActivity(intent)
    }

    //LENA: should we remove the button below to launch YAMNET -- DOESNT SEEM TO WORK

    // Launch YAMNET App
    private fun launchYamnetApp() {
        val launchIntent = packageManager.getLaunchIntentForPackage("org.tensorflow.lite.examples.audio")
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            T.showShort(mContext, "YAMNET app not found")
        }
    }

    private var exitTime: Long = 0
    override fun onBackPressed() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            T.showShort(mContext, "Press one more time to exit.")
            exitTime = System.currentTimeMillis()
        } else {
            finish()
        }
    }
}
