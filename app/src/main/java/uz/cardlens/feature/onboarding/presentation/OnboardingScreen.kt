package uz.cardlens.feature.onboarding.presentation

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import uz.cardlens.R
import uz.cardlens.ui.theme.GradientAccent
import uz.cardlens.ui.theme.GradientIndigo
import uz.cardlens.ui.theme.GradientTeal

private data class OnboardingPage(
    val titleResId: Int,
    val descriptionResId: Int,
    val icon: ImageVector,
    val gradient: List<Color>,
)

private val pages = listOf(
    OnboardingPage(
        titleResId = R.string.onboarding_title_1,
        descriptionResId = R.string.onboarding_desc_1,
        icon = Icons.Default.CameraAlt,
        gradient = GradientTeal,
    ),
    OnboardingPage(
        titleResId = R.string.onboarding_title_2,
        descriptionResId = R.string.onboarding_desc_2,
        icon = Icons.Default.Badge,
        gradient = GradientIndigo,
    ),
    OnboardingPage(
        titleResId = R.string.onboarding_title_3,
        descriptionResId = R.string.onboarding_desc_3,
        icon = Icons.Default.Event,
        gradient = GradientAccent,
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingRoute(
    onCompleted: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(Brush.linearGradient(page.gradient)),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp),
                    )
                }
                Spacer(Modifier.height(40.dp))
                Text(
                    text = stringResource(page.titleResId),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(page.descriptionResId),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier.padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            pages.indices.forEach { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .width(if (selected) 24.dp else 8.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                if (pagerState.currentPage < pages.size - 1) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    onCompleted()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                if (pagerState.currentPage < pages.size - 1) stringResource(R.string.onboarding_next) else stringResource(R.string.onboarding_get_started),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (pagerState.currentPage < pages.size - 1) {
            TextButton(onClick = onCompleted) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
