package com.example.expensetrackerapp.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.expensetrackerapp.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class GroupTabsFragment : Fragment() {

    private var groupId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_tabs, container, false)
        groupId = arguments?.getString("groupId")

        // Setup ViewPager and TabLayout
        val viewPagerGroup = view.findViewById<ViewPager2>(R.id.viewPagerGroup)
        val tabLayoutGroup = view.findViewById<TabLayout>(R.id.tabLayoutGroup)

        viewPagerGroup.adapter = GroupPagerAdapter(requireActivity(), groupId)
        TabLayoutMediator(tabLayoutGroup, viewPagerGroup) { tab, position ->
            tab.text = when (position) {
                0 -> "Overview"
                1 -> "Summary"
                else -> "Tab ${position + 1}"
            }
        }.attach()

        return view
    }

    private inner class GroupPagerAdapter(fa: FragmentActivity, private val groupId: String?) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> GroupOverviewFragment().apply {
                    arguments = Bundle().apply { putString("groupId", groupId) }
                }
                1 -> SummaryFragment().apply {
                    arguments = Bundle().apply { putString("groupId", groupId) }
                }
                else -> throw IllegalArgumentException("Invalid tab position")
            }
        }
    }
}
