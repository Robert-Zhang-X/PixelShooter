package com.pixelshooter.game.ui

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.pixelshooter.R
import com.pixelshooter.game.entities.PlaneType

class PlaneSelectActivity : AppCompatActivity() {

    private var levelId = 1
    private var selectedPlane = PlaneType.FALCON

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_plane_select)

        levelId = intent.getIntExtra("level_id", 1)

        val planeInfos = listOf(
            Triple(PlaneType.FALCON,   "猎鹰号",    "单发穿透弹 · HP:150\n技能：蓄力超级炮"),
            Triple(PlaneType.STORM,    "风暴号",    "扇形5发散射 · HP:120\n技能：旋风360°清场"),
            Triple(PlaneType.HUNTERII, "猎鹰II号", "追踪导弹 · HP:100\n技能：8枚导弹风暴")
        )

        val btnIds = listOf(R.id.btn_plane1, R.id.btn_plane2, R.id.btn_plane3)
        val descIds = listOf(R.id.tv_plane1_desc, R.id.tv_plane2_desc, R.id.tv_plane3_desc)

        planeInfos.forEachIndexed { idx, (type, name, desc) ->
            val btn = findViewById<Button>(btnIds[idx])
            val tv  = findViewById<TextView>(descIds[idx])
            btn.text = name
            tv.text  = desc
            btn.setOnClickListener {
                selectedPlane = type
                btnIds.forEachIndexed { i, id ->
                    findViewById<Button>(id).alpha = if (i == idx) 1.0f else 0.5f
                }
            }
        }

        findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("level_id", levelId)
            intent.putExtra("plane_type", selectedPlane.name)
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }
    }
}
