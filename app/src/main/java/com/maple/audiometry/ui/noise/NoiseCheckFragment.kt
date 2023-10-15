package com.maple.audiometry.ui.noise

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout.LayoutParams
import com.maple.audiometry.R
import com.maple.audiometry.ui.base.BaseFragment
import com.maple.audiometry.ui.base.BaseFragmentActivity
import com.maple.audiometry.ui.detection.DetectionActivity
import com.maple.audiometry.utils.ArrayUtils
import com.maple.audiometry.utils.MediaRecorderDemo
import com.maple.audiometry.utils.permission.RxPermissions
import com.maple.audiometry.view.BrokenLineView
import com.maple.msdialog.AlertDialog
import kotlinx.android.synthetic.main.activity_noise.*
import java.util.*

/**
 * Noise Detection
 *
 */
class NoiseCheckFragment : BaseFragment() {
    companion object {
        const val checkTime = 10000 * 1000
        const val UPDATE_NOISE_VALUE = 1
    }

    private var startTime: Long = 0
    private var media: MediaRecorderDemo? = null
    private lateinit var mBrokenLine: BrokenLineView
    private var maxVolume = 0.0
    private var minVolume = 99990.0
    private val allVolume = ArrayList<Double>()
    private lateinit var dbExplain: Array<String>
    private lateinit var mActivity: BaseFragmentActivity

    @SuppressLint("HandlerLeak")
    private val handler = Handler { msg ->
        when (msg.what) {
            UPDATE_NOISE_VALUE -> {
                val db = msg.obj as Double
                val time = System.currentTimeMillis() - startTime
                if (time >= checkTime) {
                    media?.stopRecord()
                    showDialog()
                }
                mBrokenLine.updateDate(ArrayUtils.sub(allVolume, mBrokenLine.maxCacheNum))
                updateNoise(db)
            }
        }
        true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.activity_noise, container, false)
        view.isClickable = true
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mActivity = activity as BaseFragmentActivity
        mBrokenLine = BrokenLineView(mContext)
        ll_chart_view.addView(mBrokenLine.execute(), LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        dbExplain = resources.getStringArray(R.array.db_explain_arr)
        RxPermissions(this)
            .request(Manifest.permission.RECORD_AUDIO)
            .subscribe { granted -> if (granted) handler.post(checkNoise) }
    }

    private val checkNoise = Runnable {
        media = MediaRecorderDemo(requireActivity(), object: MediaRecorderDemo.NoiseValueUpdateCallback {
            override fun onUpdateNoiseValue(noiseValue: Double) {
                val msg = Message.obtain()
                msg.what = UPDATE_NOISE_VALUE
                msg.obj = noiseValue
                handler.sendMessage(msg)
            }
        })
        media?.startRecord()
        startTime = System.currentTimeMillis()
    }

    override fun onDestroy() {
        media?.stopRecord()
        super.onDestroy()
    }

    @SuppressLint("SetTextI18n")
    private fun updateNoise(db: Double) {
        tv_noise_value.text = "${db.toInt()} dB"
        if (db > maxVolume) {
            maxVolume = db
            tv_max_value.text = "Highest:\n ${maxVolume.toInt()} dB"
        }
        if (db < minVolume && db != 0.0) {
            minVolume = db
            tv_min_value.text = "Lowest:\n ${minVolume.toInt()} dB"
        }
        if (db != 0.0) {
            allVolume.add(db)
            val avgVolume = ArrayUtils.avg(allVolume)
            tv_db_explain1.text = dbExplain[(avgVolume / 10).toInt()]
            tv_db_explain2.text = dbExplain[(avgVolume / 10).toInt() + 1]
            tv_avg_value.text = "Average:\n ${avgVolume.toInt()} dB"
        }
    }

    private fun showDialog() {
        val alertMessage = if (ArrayUtils.avg(allVolume) > 40) {
            "Your monitoring environment is not suitable for the following test. Please test in a quieter environment."
        } else {
            "Your test environment is good, you can continue the next test."
        }
        AlertDialog(mContext)
            .setScaleWidth(0.7)
            .setMessage(alertMessage)
            .setLeftButton("Cancel", null)
            .setRightButton(if (ArrayUtils.avg(allVolume) > 40) "Retest" else "Enter Test") { toCheckEar() }
            .show()
    }

    private fun toCheckEar() {
        mActivity.onBackPressed()
        val intent = Intent(mActivity, DetectionActivity::class.java)
        startActivity(intent)
    }
}
