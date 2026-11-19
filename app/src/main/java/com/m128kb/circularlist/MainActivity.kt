package com.m128kb.circularlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.m128kb.circularlist.ui.theme.CircularListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CircularListTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding)
                    ){
                        CircularSwipeList(items = sampleItems)
                    }
                }
            }
        }
    }
}

data class CircleItem(
    val id: Int,
    val title: String,
    val color: Color
)

val sampleItems = listOf(
    CircleItem(1, "Item 1", Color(0xFFE53935)),
    CircleItem(2, "Item 2", Color(0xFF1E88E5)),
    CircleItem(3, "Item 3", Color(0xFF43A047)),
    CircleItem(4, "Item 4", Color(0xFF8E24AA)),
    CircleItem(5, "Item 5", Color(0xFFFB8C01))
)

@Composable
fun CircularSwipeList(
    items: List<CircleItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    var rotation by remember(items.size) {
        mutableFloatStateOf(0f)
    }


    val scope = rememberCoroutineScope()

    var settleJob by remember {
        mutableStateOf<Job?>(null)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        val density = LocalDensity.current

        val centerX = with(density) {
            maxWidth.toPx() / 2f
        }

        val centerY = with(density) {
            maxHeight.toPx() / 2f
        }

        val radius = with(density) {
            (minOf(maxWidth, maxHeight) * 0.32f)
                .toPx()
        }

        val itemWidth = 100.dp
        val itemHeight = 100.dp

        val itemWidthPx = with(density) {
            itemWidth.toPx()
        }

        val itemHeightPx = with(density) {
            itemHeight.toPx()
        }

        val angleStep = (2f * PI.toFloat()) / items.size

        items.forEachIndexed { index, item ->

            // Calculate the item's current angle.
            val angle =
                -PI.toFloat() / 2f +
                        index * angleStep +
                        rotation

            val currentAngle = rememberUpdatedState(angle)

            // Calculate its circular position.
            val x = centerX + radius * cos(angle) -
                    itemWidthPx / 2f

            val y = centerY + radius * sin(angle) -
                    itemHeightPx / 2f

            // Make items at the top slightly larger.
            val depth = (1 - sin(angle)) / 2f
            val itemScale = 0.84f + depth * 0.16f

            // Animate toward the nearest item position.
            val settleWheel: () -> Unit = {
                val target =
                    (rotation / angleStep).roundToInt() *
                            angleStep

                settleJob?.cancel()

                settleJob = scope.launch {
                    val animatable = Animatable(rotation)

                    animatable.animateTo(
                        targetValue = target,
                        animationSpec = spring(
                            dampingRatio = 0.75f,
                            stiffness = 300f
                        )
                    ) {
                        rotation = value
                    }
                }
            }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x.roundToInt(),
                            y.roundToInt()
                        )
                    }
                    .size(itemWidth, itemHeight)
                    .zIndex(depth)
                    .scale(itemScale)
                    .pointerInput(items.size, radius) {

                        detectDragGestures(
                            onDragStart = {
                                settleJob?.cancel()
                            },

                            onDragEnd = {
                                settleWheel()
                            },

                            onDragCancel = {
                                settleWheel()
                            },

                            onDrag = { change, dragAmount ->

                                change.consume()

                                val theta = currentAngle.value

                                // Convert horizontal drag into rotation.
                                val angularDrag =
                                    (-sin(theta) * dragAmount.x +
                                            cos(theta) * dragAmount.y) / radius

                                rotation += angularDrag
                            }
                        )
                    }
                    .clip(CircleShape)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = item.color
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 6.dp
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}
