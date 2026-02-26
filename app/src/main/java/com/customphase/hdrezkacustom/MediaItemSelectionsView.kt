package com.customphase.hdrezkacustom
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.util.Predicate
import androidx.core.view.children
import com.google.android.flexbox.FlexboxLayout

class MediaItemSelectionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView : TextView by lazy {findViewById(R.id.mediaItemSelectionsTitle)}
    private val gridView : FlexboxLayout by lazy {findViewById(R.id.mediaItemSelectionsItems)}
    private val loadingView : View by lazy {findViewById(R.id.mediaItemSelectionsLoadingIndicator)}

    public var selectedItem : Pair<View?, MediaItemSelection?> = Pair(null, null)
        private set

    fun setTitle(title : String) {
        titleView.text = title
    }

    fun setSelected(item : View) {
        selectedItem.first?.isActivated = false
        item.isActivated = true
        selectedItem = Pair(
            item, item.tag as MediaItemSelection
        )
    }

    fun addItem(data : MediaItemSelection, isSelected : Boolean) : View {
        val btn = LayoutInflater.from(context).inflate(R.layout.button_radio, gridView, false) as Button
        btn.text = data.title
        btn.tag = data
        gridView.addView(btn)
        if (isSelected) setSelected(btn)
        return btn
    }

    fun setItemsVisibility(predicate: Predicate<MediaItemSelection>) {
        for(child in gridView.children) {
            val sel = child.tag as MediaItemSelection
            child.visibility = if (predicate.test(sel)) VISIBLE else GONE
        }
    }

    fun clear() {
        gridView.removeAllViews()
    }

    fun startLoading() {
        loadingView.visibility = VISIBLE
        gridView.visibility = GONE
    }

    fun stopLoading() {
        loadingView.visibility = GONE
        gridView.visibility = VISIBLE
    }

}