package io.github.wulkanowy.ui.modules.dashboard

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.wulkanowy.R
import io.github.wulkanowy.databinding.FragmentDashboardBinding
import io.github.wulkanowy.ui.base.BaseFragment
import io.github.wulkanowy.ui.modules.main.MainView
import io.github.wulkanowy.utils.toFormattedString
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class DashboardFragment : BaseFragment<FragmentDashboardBinding>(R.layout.fragment_dashboard),
    DashboardView, MainView.TitledView {

    @Inject
    lateinit var presenter: DashboardPresenter

    @Inject
    lateinit var dashboardAdapter: DashboardAdapter

    override val titleStringId get() = R.string.dashboard_title

    @SuppressLint("DefaultLocale")
    override var subtitleString =
        LocalDate.now().toFormattedString("EEEE, d MMMM yyyy").capitalize()

    companion object {

        fun newInstance() = DashboardFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDashboardBinding.bind(view)
        presenter.onAttachView(this)
    }

    override fun initView() {
        with(binding.dashboardRecycler) {
            layoutManager = LinearLayoutManager(context)
            adapter = dashboardAdapter
        }
    }

    override fun updateData(data: List<DashboardData>) {
        with(dashboardAdapter) {
            items = data
            notifyDataSetChanged()
        }
    }

    override fun updateGradeTheme(theme: String) {
        with(dashboardAdapter) {
            gradeTheme = theme
            notifyDataSetChanged()
        }
    }

    override fun showMessage(text: String) {
    }

    override fun onDestroyView() {
        presenter.onDetachView()
        super.onDestroyView()
    }
}