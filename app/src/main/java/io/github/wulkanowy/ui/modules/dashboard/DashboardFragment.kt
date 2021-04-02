package io.github.wulkanowy.ui.modules.dashboard

import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import io.github.wulkanowy.R
import io.github.wulkanowy.databinding.FragmentDashboardBinding
import io.github.wulkanowy.ui.base.BaseFragment
import io.github.wulkanowy.ui.modules.main.MainView
import javax.inject.Inject

@AndroidEntryPoint
class DashboardFragment : BaseFragment<FragmentDashboardBinding>(R.layout.fragment_dashboard),
    DashboardView, MainView.TitledView {

    @Inject
    lateinit var presenter: DashboardPresenter

    override val titleStringId get() = R.string.dashboard_title

    companion object {

        fun newInstance() = DashboardFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter.onAttachView(this)
    }

    override fun initView() {
    }

    override fun onDestroyView() {
        presenter.onDetachView()
        super.onDestroyView()
    }
}