/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package echo.music.iad1tya.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import echo.music.iad1tya.R

class AdaptiveQueueWidgetService : RemoteViewsService() {

  override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
    return AdaptiveQueueViewsFactory(applicationContext, intent)
  }

  companion object {
    @Volatile
    var queueData: List<WidgetQueueItem> = emptyList()
  }
}

class AdaptiveQueueViewsFactory(
  private val context: Context,
  private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

  private var items: List<WidgetQueueItem> = emptyList()

  override fun onCreate() {
    items = AdaptiveQueueWidgetService.queueData
  }

  override fun onDataSetChanged() {
    items = AdaptiveQueueWidgetService.queueData
  }

  override fun onDestroy() {
    items = emptyList()
  }

  override fun getCount(): Int = items.size

  override fun getViewAt(position: Int): RemoteViews? {
    if (position < 0 || position >= items.size) return null
    val item = items[position]

    val views = RemoteViews(context.packageName, R.layout.widget_queue_item)
    views.setTextViewText(R.id.queue_item_title, item.title)
    views.setTextViewText(R.id.queue_item_artist, item.artist)

    val fillInIntent = Intent().apply {
      putExtra(MusicWidgetReceiver.EXTRA_QUEUE_INDEX, item.index)
    }
    views.setOnClickFillInIntent(R.id.queue_item_root, fillInIntent)
    views.setOnClickFillInIntent(R.id.queue_item_title, fillInIntent)
    views.setOnClickFillInIntent(R.id.queue_item_artist, fillInIntent)

    return views
  }

  override fun getLoadingView(): RemoteViews? = null

  override fun getViewTypeCount(): Int = 1

  override fun getItemId(position: Int): Long = position.toLong()

  override fun hasStableIds(): Boolean = false
}
