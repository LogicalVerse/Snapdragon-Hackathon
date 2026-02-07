package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.ElectricGreen
import com.example.myapplication.ui.theme.ElevatedDark
import com.example.myapplication.ui.theme.MediumGray
import com.example.myapplication.ui.theme.OffWhite
import com.example.myapplication.ui.theme.Spacing
import com.example.myapplication.ui.theme.SubtleGray

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * GitHub-style streak heat map dynamically showing workout activity
 */
@Composable
fun StreakHeatMap(
    modifier: Modifier = Modifier,
    history: Map<LocalDate, Int> = emptyMap(),
    today: LocalDate = LocalDate.now()
) {
    // Calculate start date: 4 weeks ago, aligned to Monday
    // This ensures we show exactly 4 columns of weeks
    val startDate = remember(today) {
        today.minusWeeks(3).with(DayOfWeek.MONDAY)
    }
    
    // Calculate current streak inside composable or pass it
    // For now, let's calculate active days in sequence ending today
    val currentStreak = remember(history, today) {
        var streak = 0
        var date = today
        while (history[date] ?: 0 > 0) {
            streak++
            date = date.minusDays(1)
        }
        streak
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            // Header with streak count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OffWhite
                    )
                    Text(
                        text = "Last 4 weeks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MediumGray
                    )
                }
                
                // Streak badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "🔥",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$currentStreak day streak",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricGreen
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Heat map grid (Rows=Days, Cols=Weeks)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Day labels (M, T, W...)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Monday to Sunday
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                        Box(
                            modifier = Modifier.size(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                color = MediumGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Heat map cells
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    for (week in 0 until 4) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (dayOfWeekOrdinal in 0 until 7) { // 0=Mon, 6=Sun
                                val cellDate = startDate.plusWeeks(week.toLong()).plusDays(dayOfWeekOrdinal.toLong())
                                val intensity = history[cellDate] ?: 0
                                
                                // Don't show future days if we strictly adhere to "today" clipping? 
                                // Or show empty. Let's show empty for future.
                                val isFuture = cellDate.isAfter(today)
                                HeatMapCell(
                                    intensity = if (isFuture) 0 else intensity,
                                    isFuture = isFuture,
                                    isToday = cellDate.isEqual(today)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Less",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGray
                )
                Spacer(modifier = Modifier.width(4.dp))
                (0..3).forEach { level ->
                    HeatMapCell(intensity = level, size = 12, isExample = true)
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "More",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGray
                )
            }
        }
    }
}

@Composable
private fun HeatMapCell(
    intensity: Int,
    size: Int = 16,
    isFuture: Boolean = false,
    isToday: Boolean = false,
    isExample: Boolean = false
) {
    val baseColor = when (intensity) {
        0 -> SubtleGray
        1 -> ElectricGreen.copy(alpha = 0.3f)
        2 -> ElectricGreen.copy(alpha = 0.6f)
        else -> ElectricGreen
    }
    
    // Highlight today with a border or slightly different shade if active?
    // User requested "colour should change when I open the app" -> handled by intensity
    // But maybe visual cue for "Today" is good.
    val borderColor = if (isToday) ElectricGreen else Color.Transparent
    val borderWidth = if (isToday) 1.dp else 0.dp
    
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isFuture) Color.Transparent else baseColor)
            .then(if (isToday) Modifier.border(borderWidth, borderColor, RoundedCornerShape(3.dp)) else Modifier)
    )
}

