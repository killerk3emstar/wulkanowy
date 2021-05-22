package io.github.wulkanowy.ui.modules.grade.details

import io.github.wulkanowy.data.db.entities.Grade
import io.github.wulkanowy.ui.base.BaseView
import java.time.LocalDate

interface GradeDetailsView : BaseView {

    val isViewEmpty: Boolean

    fun initView()

    fun updateData(data: List<GradeDetailsItem>, isGradeExpandable: Boolean, gradeColorTheme: String)

    fun updateItem(item: Grade, position: Int)

    fun updateHeaderItem(item: GradeDetailsItem)

    fun clearView()

    fun scrollToStart()

    fun collapseAllItems()

    fun showGradeDialog(grade: Grade, colorScheme: String)

    fun showContent(show: Boolean)

    fun showEmpty(show: Boolean)

    fun showProgress(show: Boolean)

    fun showErrorView(show: Boolean)

    fun setErrorDetails(message: String)

    fun enableSwipe(enable: Boolean)

    fun showRefresh(show: Boolean)

    fun notifyParentDataLoaded(semesterId: Int)

    fun notifyParentRefresh()

    fun disableMarkAsDoneButton()

    fun enableMarkAsDoneButton(oldestUnread: LocalDate)

    fun getHeaderOfItem(subject: String): GradeDetailsItem
}
