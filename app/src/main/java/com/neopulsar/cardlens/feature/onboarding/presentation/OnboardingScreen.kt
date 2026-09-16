package com.neopulsar.cardlens.feature.onboarding.presentation

import com.neopulsar.cardlens.R
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.neopulsar.cardlens.ui.theme.GradientAccent
import com.neopulsar.cardlens.ui.theme.GradientIndigo
import com.neopulsar.cardlens.ui.theme.GradientTeal

private data class OnboardingPage(val titleResId: Int, val descriptionResId: Int, val icon: ImageVector, val gradient: List<Color>)

private val pages = listOf(
    OnboardingPage(R.string.onboarding_title_1, R.string.onboarding_desc_1, Icons.Default.CameraAlt, GradientTeal),
    OnboardingPage(R.string.onboarding_title_2, R.string.onboarding_desc_2, Icons.Default.Badge, GradientIndigo),
    OnboardingPage(R.string.onboarding_title_3, R.string.onboarding_desc_3, Icons.Default.Event, GradientAccent),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingRoute(onCompleted: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top skip — always visible for UX, premium placement
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.End) {
            if (pagerState.currentPage < pages.size - 1) {
                TextButton(onClick = onCompleted, shape = RoundedCornerShape(999.dp)) {
                    Text(stringResource(R.string.onboarding_skip), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else Spacer(Modifier.height(40.dp))
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Premium icon — soft shadow + border
                Box(
                    modifier = Modifier.size(128.dp).clip(RoundedCornerShape(32.dp))
                        .background(Brush.linearGradient(page.gradient))
                        .padding(1.dp).clip(RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)).background(Brush.linearGradient(page.gradient)), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(page.icon, null, tint = Color.White, modifier = Modifier.size(56.dp))
                    }
                }
                Spacer(Modifier.height(36.dp))
                Text(stringResource(page.titleResId), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(page.descriptionResId), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = MaterialTheme.typography.bodyLarge.lineHeight)
            }
        }

        Row(modifier = Modifier.padding(top = 8.dp, bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            pages.indices.forEach { index ->
                val selected = pagerState.currentPage == index
                val width by animateDpAsState(if (selected) 28.dp else 8.dp, label = "dot")
                Box(
                    modifier = Modifier.height(8.dp).width(width).clip(RoundedCornerShape(999.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                )
            }
        }

        Button(
            onClick = {
                if (pagerState.currentPage < pages.size - 1) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } else onCompleted()
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        ) {
            Text(
                if (pagerState.currentPage < pages.size - 1) stringResource(R.string.onboarding_next) else stringResource(R.string.onboarding_get_started),
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(Modifier.height(18.dp))
        Text("Swipe to explore", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        Spacer(Modifier.height(28.dp))
    }
}
