package com.tutr.backend.dto.rating;

import com.tutr.backend.model.enums.CourseCategory;
import com.tutr.backend.model.enums.TeachingMode;
import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class FilterOptions {
    private Map<CourseCategory, Long> categoryCounts;
    private Map<CourseCategory, Double> categoryAverages;
    private Map<TeachingMode, Long> teachingModeCounts;
    private Map<TeachingMode, Double> teachingModeAverages;
}