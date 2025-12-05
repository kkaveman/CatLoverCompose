package com.example.catlovercompose.feature.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.catlovercompose.feature.screens.community.info.InformationScreen
import com.example.catlovercompose.feature.screens.community.postsection.postlist.PostListScreen
import com.example.catlovercompose.feature.screens.community.adoption.AdoptionScreen
import kotlinx.coroutines.launch

@Composable
fun CommunityScreen() {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    val tabs = listOf("Posts", "Adoption Center","Information")

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Row at the top
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

        // Horizontal Pager for content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> PostListScreen()
                1 -> AdoptionScreen()
                2 -> InformationScreen()

            }
        }
    }
}