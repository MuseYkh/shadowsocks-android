package com.github.shadowsocks

import android.app.Activity
import android.content.pm.ShortcutManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.core.content.getSystemService
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.shadowsocks.Core.app
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.utils.resolveResourceId
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.android.synthetic.main.layout_quick_switch.*

/**
 * @author: wanshi
 * created on: 2019-08-26 14:07
 * description:
 */
class QuickSwitchActivity : Activity(), ShadowsocksConnection.Callback {

    private val profiles by lazy { ProfileManager.getAllProfiles() }
    private val connection by lazy { ShadowsocksConnection() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_quick_switch)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setTitle(R.string.quick_switch)

        val layoutManager = LinearLayoutManager(this)
        profilesList.layoutManager = layoutManager
        profilesList.itemAnimator = DefaultItemAnimator()
        profilesList.adapter = ProfileAdapter()
        Core.activeProfileIds.firstOrNull()?.let {
            profiles?.forEach { profile ->
                if (profile.id == it) {
                    layoutManager.smoothScrollToPosition(profilesList, null, profiles!!.indexOf(profile))
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 25) getSystemService<ShortcutManager>()?.reportShortcutUsed("toggle")
    }

    inner class ProfileAdapter : RecyclerView.Adapter<ProfileViewHolder>() {

        private val name = "select_dialog_singlechoice_" + (if (Build.VERSION.SDK_INT >= 21) "material" else "holo")

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
            return ProfileViewHolder(LayoutInflater.from(parent.context).inflate(Resources.getSystem()
                    .getIdentifier(name, "layout", "android"), parent, false))
        }

        override fun getItemCount(): Int = profiles?.size ?: 0

        override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
            profiles?.let {
                holder.bind(profiles!![position])
            }
        }
    }

    inner class ProfileViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        private var item: Profile? = null
        private val text = itemView.findViewById<CheckedTextView>(android.R.id.text1)

        init {
            view.setBackgroundResource(theme.resolveResourceId(android.R.attr.selectableItemBackground))
            itemView.setOnClickListener(this)
        }

        fun bind(item: Profile) {
            this.item = item
            text.text = item.formattedName
            text.isChecked = Core.activeProfileIds.contains(item.id)
        }

        override fun onClick(v: View?) {
            item?.run {
                Core.switchProfile(id)
                connection.connect(this@QuickSwitchActivity, this@QuickSwitchActivity)
                finish()
            }
        }
    }

    override fun onDestroy() {
        connection.disconnect(this)
        super.onDestroy()
    }

    override fun onServiceConnected(service: IShadowsocksService) {
        val state = BaseService.State.values()[service.state]
        when {
            state.canStop -> Core.reloadService()
            state == BaseService.State.Stopped -> Core.startService()
        }
        finish()
    }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {

    }
}
