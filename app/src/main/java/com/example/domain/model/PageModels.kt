package com.example.domain.model

data class PageModel(
    val id: String,
    val iconEmoji: String,
    val name: String,
    val description: String,
    val bgHex: Long,
    val accentHex: Long,
    val style: String,
    val group: String,
    val visualGuidance: String
) {
    val isFactual: Boolean
        get() = id != "vidas-ficcao"
}

object PageModelsCatalog {
    val all: List<PageModel> = listOf(
        PageModel(
            id = "historia",
            iconEmoji = "🏛️",
            name = "História",
            description = "Pergaminho, mapas antigos, gravuras e paleta sépia.",
            bgHex = 0xFFEAD9B5,
            accentHex = 0xFF8D542D,
            style = "serif",
            group = "Galeria tradicional",
            visualGuidance = "Direção visual histórica: pergaminhos, mapas antigos, gravuras e paleta sépia; composição documental elegante."
        ),
        PageModel(
            id = "curiosidades",
            iconEmoji = "💡",
            name = "Curiosidades",
            description = "Pop de figurinha, retícula amarela, emojis e cartões vibrantes.",
            bgHex = 0xFFFFD83D,
            accentHex = 0xFFFF477E,
            style = "rounded",
            group = "Galeria tradicional",
            visualGuidance = "Direção visual pop e didática: amarelo vivo, formas gráficas, emojis pontuais e alto contraste; manter legibilidade."
        ),
        PageModel(
            id = "como-funciona-3d",
            iconEmoji = "⚙️",
            name = "Como funciona (3D)",
            description = "Planta técnica azul, grade e ilustrações tridimensionais.",
            bgHex = 0xFF0B3A6E,
            accentHex = 0xFFFFD166,
            style = "technical",
            group = "Galeria tradicional",
            visualGuidance = "Direção técnica: fundo azul, grade de desenho, ilustrações 3D explicativas e aparência de planta técnica."
        ),
        PageModel(
            id = "corpo-humano-saude",
            iconEmoji = "🫀",
            name = "Corpo humano e saúde",
            description = "Visual clínico claro, anatomia 3D e diagramas explicativos.",
            bgHex = 0xFFEAF5F4,
            accentHex = 0xFF168F88,
            style = "clean",
            group = "Galeria tradicional",
            visualGuidance = "Direção clínica: fundo claro, anatomia 3D educativa, precisão visual e linguagem não sensacionalista."
        ),
        PageModel(
            id = "financas",
            iconEmoji = "💰",
            name = "Finanças",
            description = "Terminal financeiro escuro, números e gráficos de mercado.",
            bgHex = 0xFF07130F,
            accentHex = 0xFF00E59B,
            style = "mono",
            group = "Galeria tradicional",
            visualGuidance = "Direção financeira: terminal escuro, números e gráficos discretos; nunca inventar valores ou promessas."
        ),
        PageModel(
            id = "noticias-explicadas",
            iconEmoji = "📰",
            name = "Notícias explicadas",
            description = "Telejornal editorial, manchetes curtas e faixas de notícia.",
            bgHex = 0xFF092B54,
            accentHex = 0xFFFF4C5E,
            style = "news",
            group = "Galeria tradicional",
            visualGuidance = "Direção editorial jornalística: enquadramento de telejornal, faixas discretas e linguagem visual informativa."
        ),
        PageModel(
            id = "misterios",
            iconEmoji = "🕵️",
            name = "Mistérios",
            description = "Arquivo confidencial, carimbos, fotografias e documentos.",
            bgHex = 0xFF0C0C14,
            accentHex = 0xFFF34A4A,
            style = "typewriter",
            group = "Galeria tradicional",
            visualGuidance = "Direção de arquivo investigativo: documentos, carimbos, fotografias e atmosfera de mistério sem fabricar evidências."
        ),
        PageModel(
            id = "ciencia-espaco",
            iconEmoji = "🚀",
            name = "Ciência e espaço",
            description = "Cosmos, órbitas, estrelas e tipografia futurista.",
            bgHex = 0xFF080D28,
            accentHex = 0xFF70C8FF,
            style = "space",
            group = "Galeria tradicional",
            visualGuidance = "Direção espacial: cosmos profundo, órbitas, estrelas e elementos científicos visualmente plausíveis."
        ),
        PageModel(
            id = "anuncio-interface",
            iconEmoji = "🧩",
            name = "Anúncio de interface",
            description = "Telas de produto, componentes e chamadas comerciais discretas.",
            bgHex = 0xFF171327,
            accentHex = 0xFF9B75FF,
            style = "ui",
            group = "Galeria tradicional",
            visualGuidance = "Direção de produto: interfaces limpas, cartões de demonstração e foco em funcionalidades reais."
        ),
        PageModel(
            id = "bichos-3d",
            iconEmoji = "🐾",
            name = "Bichos em 3D",
            description = "Animais em 3D no próprio ambiente, com cenas expressivas.",
            bgHex = 0xFF132018,
            accentHex = 0xFF8DDC73,
            style = "rounded",
            group = "Galeria tradicional",
            visualGuidance = "Direção de animação 3D: animais expressivos em ambientes naturais coerentes, sem antropomorfismo excessivo."
        ),
        PageModel(
            id = "motivacao-habitos",
            iconEmoji = "🔥",
            name = "Motivação e hábitos",
            description = "Tipografia forte, frases curtas e contraste cinematográfico.",
            bgHex = 0xFF171717,
            accentHex = 0xFFFF5B32,
            style = "bold",
            group = "Galeria tradicional",
            visualGuidance = "Direção tipográfica motivacional: contraste forte, imagens cinematográficas e frases curtas apenas na edição."
        ),
        PageModel(
            id = "tecnologia",
            iconEmoji = "💻",
            name = "Tecnologia",
            description = "Placa de circuito, linhas digitais e ciano luminoso.",
            bgHex = 0xFF07151B,
            accentHex = 0xFF19D9E8,
            style = "tech",
            group = "Galeria tradicional",
            visualGuidance = "Direção tecnológica: circuitos, linhas digitais, ciano luminoso e dispositivos plausíveis."
        ),
        PageModel(
            id = "inteligencia-artificial",
            iconEmoji = "🤖",
            name = "Inteligência artificial",
            description = "Janela de conversa, interface de IA e elementos digitais.",
            bgHex = 0xFF111020,
            accentHex = 0xFFAD8CFF,
            style = "ui",
            group = "Galeria tradicional",
            visualGuidance = "Direção de interface de IA: janelas de conversa, elementos digitais e visual contemporâneo."
        ),
        PageModel(
            id = "renda-extra-negocios",
            iconEmoji = "📈",
            name = "Renda extra e negócios",
            description = "Planilha limpa, indicadores e números destacados.",
            bgHex = 0xFFEFF4ED,
            accentHex = 0xFF20A85A,
            style = "clean",
            group = "Galeria tradicional",
            visualGuidance = "Direção de planilha: indicadores claros, tabelas limpas e números verificáveis."
        ),
        PageModel(
            id = "imoveis",
            iconEmoji = "🏠",
            name = "Imóveis",
            description = "Plantas arquitetônicas, comparações e visual de projeto.",
            bgHex = 0xFFF3EEE3,
            accentHex = 0xFFD59B54,
            style = "serif",
            group = "Galeria tradicional",
            visualGuidance = "Direção arquitetônica: plantas, maquetes, interiores e perspectiva de projeto."
        ),
        PageModel(
            id = "terror-lendas-urbanas",
            iconEmoji = "👻",
            name = "Terror e lendas urbanas",
            description = "Fita VHS, ruído analógico, sombras e suspense.",
            bgHex = 0xFF09090D,
            accentHex = 0xFFE63D5A,
            style = "horror",
            group = "Galeria tradicional",
            visualGuidance = "Direção VHS: ruído analógico, sombras, baixa iluminação e suspense sem gore explícito."
        ),
        PageModel(
            id = "mitologia-folclore",
            iconEmoji = "🐉",
            name = "Mitologia e folclore",
            description = "Moldura dourada, ornamentos e ilustrações lendárias.",
            bgHex = 0xFF17100B,
            accentHex = 0xFFD7A64B,
            style = "serif",
            group = "Galeria tradicional",
            visualGuidance = "Direção lendária: ornamentos dourados, texturas antigas e ilustrações mitológicas assumidamente artísticas."
        ),
        PageModel(
            id = "explicado-1-minuto",
            iconEmoji = "⏱️",
            name = "Explicado em 1 minuto",
            description = "Caderno, marca-texto e explicações rápidas em etapas.",
            bgHex = 0xFFF7F2E7,
            accentHex = 0xFFE84B40,
            style = "hand",
            group = "Galeria tradicional",
            visualGuidance = "Direção de caderno: papel, marca-texto, esquemas simples e ritmo de explicação rápida."
        ),
        PageModel(
            id = "games",
            iconEmoji = "🎮",
            name = "Games",
            description = "Pixel art, HUD e estética retrô de videogame.",
            bgHex = 0xFF17172C,
            accentHex = 0xFF63E36F,
            style = "pixel",
            group = "Galeria tradicional",
            visualGuidance = "Direção pixel art: estética retrô, HUD e elementos de videogame."
        ),
        PageModel(
            id = "quiz-desafios",
            iconEmoji = "❓",
            name = "Quiz e desafios",
            description = "Programa de auditório, alternativas e placar.",
            bgHex = 0xFF20104B,
            accentHex = 0xFFFFDA4B,
            style = "show",
            group = "Galeria tradicional",
            visualGuidance = "Direção de auditório: placar, alternativas e cores de competição."
        ),
        PageModel(
            id = "top5-rankings",
            iconEmoji = "🏆",
            name = "Top 5 e rankings",
            description = "Pódio, posições numeradas e medalhas.",
            bgHex = 0xFF101014,
            accentHex = 0xFFFFB62E,
            style = "bold",
            group = "Galeria tradicional",
            visualGuidance = "Direção de pódio: posições numeradas, medalhas e hierarquia visual."
        ),
        PageModel(
            id = "historias-fundo-satisfatorio",
            iconEmoji = "🌀",
            name = "Histórias com fundo satisfatório",
            description = "Animação hipnótica abstrata e narração em primeiro plano.",
            bgHex = 0xFF10101C,
            accentHex = 0xFFA58CFF,
            style = "minimal",
            group = "Galeria tradicional",
            visualGuidance = "Direção minimalista hipnótica: animações abstratas suaves e baixa interferência visual."
        ),
        PageModel(
            id = "mapas-geografia",
            iconEmoji = "🗺️",
            name = "Mapas e geografia",
            description = "Cartografia, rotas, mapas e destaques territoriais.",
            bgHex = 0xFF0C2634,
            accentHex = 0xFF42C5E5,
            style = "map",
            group = "Galeria tradicional",
            visualGuidance = "Direção cartográfica: mapas, rotas e destaques territoriais geograficamente corretos."
        ),
        PageModel(
            id = "autoridade-vendas",
            iconEmoji = "🎯",
            name = "Autoridade e vendas",
            description = "Editorial minimalista, tipografia e música, sem excesso visual.",
            bgHex = 0xFFF4F0E7,
            accentHex = 0xFF292929,
            style = "editorial",
            group = "Galeria tradicional",
            visualGuidance = "Direção editorial minimalista: tipografia refinada, espaço negativo e foco na mensagem."
        ),
        PageModel(
            id = "vidas-ficcao",
            iconEmoji = "🎬",
            name = "Vidas e Cotidiano — Ficção",
            description = "Narrativa cinematográfica ficcional, personagens consistentes e imagens em tela cheia.",
            bgHex = 0xFF17151A,
            accentHex = 0xFFE8B96C,
            style = "cinema",
            group = "Novos formatos",
            visualGuidance = "Formato cinematográfico de ficção: imagens Gemini em tela cheia 16:9, cenas realistas, continuidade rigorosa de personagens, câmera suave, sem gráficos ou textos grandes."
        ),
        PageModel(
            id = "vidas-doc",
            iconEmoji = "🌍",
            name = "Vidas e Cotidiano — Documentário",
            description = "Documentário de culturas, profissões e lugares, com fatos e fontes.",
            bgHex = 0xFF101C20,
            accentHex = 0xFF7DC9B3,
            style = "documentary",
            group = "Novos formatos",
            visualGuidance = "Documentário de vidas e cotidiano: imagens ilustrativas em tela cheia 16:9, retratar culturas, profissões e lugares com respeito, não apresentar reconstruções como fotos documentais autênticas."
        ),
        PageModel(
            id = "true-crime",
            iconEmoji = "🔎",
            name = "True Crime",
            description = "Investigação documental, cronologia, fontes e cuidado com vítimas e suspeitos.",
            bgHex = 0xFF121216,
            accentHex = 0xFFC94D55,
            style = "crime",
            group = "Novos formatos",
            visualGuidance = "Documentário investigativo True Crime: imagens ilustrativas em tela cheia 16:9, cronologia visual sóbria, sem gore, sem dramatizar vítimas, sem apresentar suspeitos como culpados sem condenação."
        )
    )

    fun findById(id: String): PageModel =
        all.firstOrNull { it.id == id } ?: all.first { it.id == "vidas-ficcao" }
}
