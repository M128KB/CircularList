package com.m128kb.circularlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.m128kb.circularlist.ui.theme.CircularListTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
