package com.sowarapp.android.data

import com.sowarapp.android.R
import com.sowarapp.android.data.model.Category
import com.sowarapp.android.data.model.SowarImage

/**
 * The app's built-in catalog. All backgrounds were generated for this project,
 * so there are no copyright concerns. In a later phase this class can be swapped
 * for a remote data source without touching the screens.
 */
class LocalImageDataSource {

    val categories: List<Category> = listOf(
        Category(GOOD_MORNING, "صباح الخير", R.drawable.img_morning_1),
        Category(GOOD_EVENING, "مساء الخير", R.drawable.img_evening_1),
        Category(FLOWERS, "ورود", R.drawable.img_flowers_1),
        Category(WISDOM, "حكم وعبر", R.drawable.img_wisdom_1),
        Category(JUMUAH, "جمعة مباركة", R.drawable.img_jumuah_1),
        Category(OCCASIONS, "مناسبات", R.drawable.img_occasions_1),
    )

    val images: List<SowarImage> = listOf(
        SowarImage("morning_1", GOOD_MORNING, R.drawable.img_morning_1, "صباح الخير", isPopular = true),
        SowarImage("morning_2", GOOD_MORNING, R.drawable.img_morning_2, "صباحكم سعادة وتفاؤل"),
        SowarImage("morning_3", GOOD_MORNING, R.drawable.img_morning_3, "صباح الورد والياسمين"),
        SowarImage("morning_4", GOOD_MORNING, R.drawable.img_morning_4, "يوم جديد وأمل جديد"),
        SowarImage("morning_5", GOOD_MORNING, R.drawable.img_morning_5, "صباحك أجمل"),

        SowarImage("evening_1", GOOD_EVENING, R.drawable.img_evening_1, "مساء الخير", isPopular = true),
        SowarImage("evening_2", GOOD_EVENING, R.drawable.img_evening_2, "مساء الهدوء والسكينة"),
        SowarImage("evening_3", GOOD_EVENING, R.drawable.img_evening_3, "مساؤكم سعيد"),
        SowarImage("evening_4", GOOD_EVENING, R.drawable.img_evening_4, "ليلة هادئة وأحلام سعيدة"),
        SowarImage("evening_5", GOOD_EVENING, R.drawable.img_evening_5, "مساء الورد"),

        SowarImage("flowers_1", FLOWERS, R.drawable.img_flowers_1, "باقة ورد لك"),
        SowarImage("flowers_2", FLOWERS, R.drawable.img_flowers_2, "ورود الربيع", isPopular = true),
        SowarImage("flowers_3", FLOWERS, R.drawable.img_flowers_3, "جمال الطبيعة"),
        SowarImage("flowers_4", FLOWERS, R.drawable.img_flowers_4, "زهرة لأجمل قلب"),
        SowarImage("flowers_5", FLOWERS, R.drawable.img_flowers_5, "عطر الورد"),

        SowarImage("wisdom_1", WISDOM, R.drawable.img_wisdom_1, "الصبر مفتاح الفرج", isPopular = true),
        SowarImage("wisdom_2", WISDOM, R.drawable.img_wisdom_2, "من جدّ وجد"),
        SowarImage("wisdom_3", WISDOM, R.drawable.img_wisdom_3, "العلم نور"),
        SowarImage("wisdom_4", WISDOM, R.drawable.img_wisdom_4, "خير الكلام ما قلّ ودلّ"),
        SowarImage("wisdom_5", WISDOM, R.drawable.img_wisdom_5, "اصنع من كل تحدٍّ فرصة"),

        SowarImage("jumuah_1", JUMUAH, R.drawable.img_jumuah_1, "جمعة مباركة", isPopular = true),
        SowarImage("jumuah_2", JUMUAH, R.drawable.img_jumuah_2, "جمعة طيبة عليكم"),
        SowarImage("jumuah_3", JUMUAH, R.drawable.img_jumuah_3, "اللهم اجعلها جمعة خير وبركة"),
        SowarImage("jumuah_4", JUMUAH, R.drawable.img_jumuah_4, "جمعة مباركة على الجميع"),
        SowarImage("jumuah_5", JUMUAH, R.drawable.img_jumuah_5, "يوم الجمعة نور وسرور"),

        SowarImage("occasions_1", OCCASIONS, R.drawable.img_occasions_1, "عيد مبارك", isPopular = true),
        SowarImage("occasions_2", OCCASIONS, R.drawable.img_occasions_2, "كل عام وأنتم بخير"),
        SowarImage("occasions_3", OCCASIONS, R.drawable.img_occasions_3, "عيد ميلاد سعيد"),
        SowarImage("occasions_4", OCCASIONS, R.drawable.img_occasions_4, "ألف مبروك"),
        SowarImage("occasions_5", OCCASIONS, R.drawable.img_occasions_5, "رمضان كريم"),
    )

    companion object {
        const val GOOD_MORNING = "good_morning"
        const val GOOD_EVENING = "good_evening"
        const val FLOWERS = "flowers"
        const val WISDOM = "wisdom"
        const val JUMUAH = "jumuah"
        const val OCCASIONS = "occasions"
    }
}
