package com.example.catlovercompose.feature.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.catlovercompose.feature.screens.community.info.InformationScreen
import com.example.catlovercompose.feature.screens.community.postsection.postlist.PostListScreen
import com.example.catlovercompose.feature.screens.community.adoption.AdoptionScreen
import kotlinx.coroutines.launch

@Composable
fun CommunityScreen(navController: NavController) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val tabs = listOf("Posts", "Adoption Center", "Information")

    // ⭐ Use Box instead of Column to layer tabs on top
    Box(modifier = Modifier.fillMaxSize()) {
        // Content - fills entire space
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = pagerState.currentPage != 1 // Disable swipe on Adoption (map) page
        ) { page ->
            when (page) {
                0 -> {

                    Box(modifier = Modifier.fillMaxSize().padding(top = 48.dp)) {
                        PostListScreen(navController)
                    }
                }
                1 -> {

                    Box(modifier = Modifier.fillMaxSize().padding(top = 48.dp)) {
                        AdoptionScreen()
                    }
                }
                2 -> {

                    Box(modifier = Modifier.fillMaxSize().padding(top = 48.dp)) {
                        InformationScreen()
                    }
                }
            }
        }

        // ⭐ Tabs elevated on top with high z-index
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(100f), // Very high z-index to stay on top
            shadowElevation = 8.dp,
            tonalElevation = 3.dp
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(text = title) }
                    )
                }
            }
        }
    }
}