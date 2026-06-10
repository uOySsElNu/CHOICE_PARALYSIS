package com.choiceparalysis.turntable.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.R
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.data.model.HistoryEntry
import com.choiceparalysis.turntable.data.repository.HistoryRepository
import com.choiceparalysis.turntable.widget.WidgetDataSync
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * 答案之书 — 基于关键词场景匹配的中性决策回答引擎。
 *
 * 设计原则：
 * - 不带偏向：不说"做/不做"，而是引导思考
 * - 场景贴合：通过关键词检测问题类型，给出相关回答
 * - 温暖中性：不制造焦虑，鼓励用户自己判断
 */
object AnswerBook {

    /** 场景定义：关键词列表 + 对应回答池（@StringRes） */
    data class Scene(
        val keywords: List<String>,
        val answers: List<Int>, // @StringRes
    )

    private val scenes = listOf(
        // 饮食 (中/英/日/韩/西/法/德/俄/葡/阿/繁中)
        Scene(
            keywords = listOf(
                "吃", "喝", "餐", "饭", "菜", "火锅", "烧烤", "奶茶", "咖啡", "外卖", "食堂", "餐厅", // 中文
                "eat", "drink", "food", "lunch", "dinner", "cook", "meal", "restaurant", // English
                "食", "食べ", "飲み", "ランチ", "ディナー", "料理", "レストラン", // 日本語
                "먹", "마시", "점심", "저녁", "음식", "요리", "식당", "카페", // 한국어
                "comer", "beber", "comida", "almuerzo", "cena", "restaurante", "cocinar", // Español
                "manger", "boire", "nourriture", "déjeuner", "dîner", "restaurant", "cuisine", // Français
                "essen", "trinken", "Essen", "Mittagessen", "Abendessen", "Restaurant", "kochen", // Deutsch
                "есть", "пить", "еда", "обед", "ужин", "ресторан", "готовить", // Русский
                "comer", "beber", "comida", "almoço", "jantar", "restaurante", "cozinhar", // Português
                "أكل", "شرب", "طعام", "غداء", "عشاء", "مطعم", "طبخ", // العربية
                "食", "飲", "餐", "飯", "菜", "餐廳", // 繁體中文
            ),
            answers = listOf(
                R.string.answer_food_1,
                R.string.answer_food_2,
                R.string.answer_food_3,
                R.string.answer_food_4,
            )
        ),
        // 出行/活动 (中/英/日/韩/西/法/德/俄/葡/阿/繁中)
        Scene(
            keywords = listOf(
                "去", "出门", "旅行", "玩", "逛", "走", "旅游", "周末", "假期", // 中文
                "go", "travel", "play", "visit", "trip", "vacation", "weekend", "outing", // English
                "行く", "出掛", "旅行", "遊び", "週末", "休暇", "散歩", // 日本語
                "가", "나가", "여행", "놀", "주말", "휴가", "산책", // 한국어
                "ir", "salir", "viaje", "viajar", "jugar", "visitar", "fin de semana", "vacaciones", // Español
                "aller", "sortir", "voyage", "visiter", "jouer", "week-end", "vacances", // Français
                "gehen", "ausgehen", "Reise", "spielen", "besuchen", "Wochenende", "Urlaub", // Deutsch
                "идти", "выходить", "путешествие", "играть", "посещать", "выходные", "отпуск", // Русский
                "ir", "sair", "viagem", "viajar", "jogar", "visitar", "fim de semana", "férias", // Português
                "اذهب", "أخرج", "سفر", "سياحة", "نزهة", "عطلة", "نهاية الأسبوع", // العربية
                "去", "出門", "旅行", "玩", "逛", "週末", "假期", // 繁體中文
            ),
            answers = listOf(
                R.string.answer_outing_1,
                R.string.answer_outing_2,
                R.string.answer_outing_3,
                R.string.answer_outing_4,
            )
        ),
        // 购物 (中/英/日/韩/西/法/德/俄/葡/阿/繁中)
        Scene(
            keywords = listOf(
                "买", "购", "下单", "入手", "要不要买", "值不值", "便宜", "打折", // 中文
                "buy", "purchase", "order", "shop", "shopping", "worth", "cheap", "sale", // English
                "買う", "購入", "注文", "買い物", "セール", "安い", // 日本語
                "사", "구매", "주문", "쇼핑", "세일", "싸", // 한국어
                "comprar", "pedido", "compras", "oferta", "rebajas", "barato", // Español
                "acheter", "commande", "shopping", "promo", "solde", "pas cher", // Français
                "kaufen", "bestellen", "Einkauf", "Angebot", "Sale", "billig", // Deutsch
                "купить", "заказ", "покупки", "распродажа", "скидка", "дёшево", // Русский
                "comprar", "pedido", "compras", "oferta", "promoção", "barato", // Português
                "شراء", "شراء", "تسوق", "عرض", "تخفيض", "رخيص", // العربية
                "買", "購", "下單", "入手", "打折", // 繁體中文
            ),
            answers = listOf(
                R.string.answer_shopping_1,
                R.string.answer_shopping_2,
                R.string.answer_shopping_3,
                R.string.answer_shopping_4,
            )
        ),
        // 工作/学习 (中/英/日/韩/西/法/德/俄/葡/阿/繁中)
        Scene(
            keywords = listOf(
                "工作", "加班", "辞职", "跳槽", "学", "考试", "项目", "面试", "实习", "考研", "上班", // 中文
                "job", "work", "study", "exam", "career", "interview", "overtime", "quit", "project", // English
                "仕事", "残業", "辞職", "転職", "勉強", "試験", "面接", "就職", // 日本語
                "일", "야근", "사직", "이직", "공부", "시험", "면접", "취업", // 한국어
                "trabajo", "empleo", "estudiar", "examen", "entrevista", "renunciar", "proyecto", // Español
                "travail", "emploi", "étudier", "examen", "entretien", "démissionner", "projet", // Français
                "Arbeit", "Job", "studieren", "Prüfung", "Bewerbung", "kündigen", "Projekt", // Deutsch
                "работа", "учёба", "экзамен", "собеседование", "увольнение", "проект", // Русский
                "trabalho", "emprego", "estudar", "exame", "entrevista", "pedir demissão", "projeto", // Português
                "عمل", "وظيفة", "دراسة", "امتحان", "مقابلة", "استقالة", "مشروع", // العربية
                "工作", "加班", "辭職", "跳槽", "學", "考試", "項目", "面試", "實習", // 繁體中文
            ),
            answers = listOf(
                R.string.answer_work_1,
                R.string.answer_work_2,
                R.string.answer_work_3,
                R.string.answer_work_4,
            )
        ),
        // 感情 (中/英/日/韩/西/法/德/俄/葡/阿/繁中)
        Scene(
            keywords = listOf(
                "喜欢", "爱", "表白", "分手", "约会", "他", "她", "恋爱", "暗恋", "对象", "男友", "女友", // 中文
                "love", "like", "date", "crush", "relationship", "boyfriend", "girlfriend", "break up", "confess", // English
                "好き", "愛", "告白", "別れる", "デート", "恋人", "片思い", "彼氏", "彼女", // 日本語
                "좋아", "사랑", "고백", "헤어지", "데이트", "연인", "짝사랑", "남친", "여친", // 한국어
                "amar", "querer", "cita", "enamorado", "novio", "novia", "romper", "confesar", "relación", // Español
                "aimer", "amour", "rendez-vous", "copain", "copine", "rompre", "avouer", "relation", // Français
                "lieben", "mögen", "Date", "Schwarm", "Freund", "Freundin", "Beziehung", "Schluss machen", // Deutsch
                "любить", "нравиться", "свидание", "влюблённость", "парень", "девушка", "расставание", // Русский
                "amar", "gostar", "encontro", "namorado", "namorada", "terminar", "confessar", "relacionamento", // Português
                "حب", "أحب", "موعد", "حبيبي", "حبيبة", "انفصال", "اعتراف", "علاقة", // العربية
                "喜歡", "愛", "表白", "分手", "約會", "戀愛", "暗戀", "對象", // 繁體中文
            ),
            answers = listOf(
                R.string.answer_love_1,
                R.string.answer_love_2,
                R.string.answer_love_3,
                R.string.answer_love_4,
            )
        ),
    )

    /** 通用回答池（@StringRes），与 WidgetDataSync.generalAnswers 共享 */
    private val generalAnswers = WidgetDataSync.generalAnswers

    /**
     * 根据问题内容返回一条答案的 @StringRes ID。
     * 优先匹配场景，无匹配时使用通用池。
     */
    fun getAnswer(question: String): Int {
        if (question.isBlank()) {
            return generalAnswers.random()
        }
        val lower = question.lowercase()
        val matchedScene = scenes.firstOrNull { scene ->
            scene.keywords.any { keyword -> lower.contains(keyword) }
        }
        return matchedScene?.answers?.random() ?: generalAnswers.random()
    }
}

@HiltViewModel
class YesNoViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    private val _customQuestion = MutableStateFlow("")
    val customQuestion: StateFlow<String> = _customQuestion.asStateFlow()

    fun updateQuestion(question: String) {
        _customQuestion.value = question
    }


    fun decide() {
        if (_isAnimating.value) return
        _isAnimating.value = true
        _result.value = null

        viewModelScope.launch {
            delay(1500.milliseconds) // 翻书动画时间
            val answerRes = AnswerBook.getAnswer(_customQuestion.value)
            val answerText = appContext.getString(answerRes)
            _result.value = answerText
            _isAnimating.value = false

            historyRepository.addEntry(
                HistoryEntry(
                    method = DecisionMethod.YES_NO,
                    options = listOf(appContext.getString(R.string.method_answer_book)),
                    result = answerText,
                )
            )
            WidgetDataSync.updateLastResultWidget(
                appContext, answerText, appContext.getString(R.string.method_answer_book)
            )
        }
    }

    fun clearResult() {
        _result.value = null
    }
}
