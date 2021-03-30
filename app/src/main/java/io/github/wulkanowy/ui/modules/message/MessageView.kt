package io.github.wulkanowy.ui.modules.message

import io.github.wulkanowy.ui.base.BaseView

interface MessageView : BaseView {

    val currentPageIndex: Int

    val onlyUnread: Boolean

    val onlyWithAttachments: Boolean

    fun initView()

    fun showContent(show: Boolean)

    fun showProgress(show: Boolean)

    fun notifyChildLoadData(
        index: Int,
        forceRefresh: Boolean,
        onlyUnread: Boolean,
        onlyWithAttachments: Boolean
    )

    fun notifyChildMessageDeleted(tabId: Int)

    fun openSendMessage()
}
