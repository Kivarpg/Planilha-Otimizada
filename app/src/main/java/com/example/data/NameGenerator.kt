package com.example.data

import com.example.model.BoxNames

import kotlin.random.Random

// Culturas de nome disponíveis pro Gerador de Encontros (Aba 11). Pensado
// pra refletir os vários análogos culturais de Criação (o cenário de
// Exalted) — o Reino/Ilha Abençoada (mistura mediterrânea/europeia), o
// Leste de água (inspirado no Japão/China), o Sul de fogo (inspirado na
// África), as tribos icewalker e outras do Norte/Oeste (inspiradas em
// povos nômades/tribais) — sem se prender a uma nação real específica.
enum class CulturaNome {
    JAPONESA, CHINESA, COREANA, ISLANDESA, IRLANDESA_GAELICA, HUNGARA, AFRICANA, ARABE, MESOAMERICANA
}

enum class GeneroNome {
    MASCULINO, FEMININO
}

// Bancos de nomes usados pelo Gerador de Encontros. Entradas passam por
// curadoria conservadora: termos sagrados, classificações culturais claramente
// incorretas e categorias monolíticas sem atribuição confiável não entram no sorteio.
object NameGenerator {

    private val JAPONES_M = listOf(
        "Haruto", "Kaito", "Sora", "Ren", "Yuto", "Daiki", "Takeshi", "Kenji",
        "Hiroshi", "Ryo", "Shin", "Akira", "Tatsuya", "Noboru", "Isamu", "Kazuki",
        "Susumu", "Jiro", "Masato", "Yoshiro", "Hayato", "Riku", "Yamato", "Sota",
        "Yuki", "Kenta", "Shota", "Daisuke", "Takumi", "Hiroto", "Ryota", "Kosuke",
        "Naoki", "Yuji", "Tomoya", "Shinji", "Katsuo", "Minoru", "Osamu", "Ichiro",
        "Saburo", "Goro", "Rokuro", "Toru", "Wataru", "Makoto", "Tadashi", "Hideo",
        "Kiyoshi", "Akio", "Toshio", "Yasuo", "Norio", "Fumio", "Haruo", "Kazuo",
        "Masao", "Mitsuo", "Nobuo", "Sadao", "Shigeo", "Takeo", "Tomio", "Yoshio",
        "Eiji", "Koji", "Kenichi", "Shinichi", "Yoichi", "Junichi", "Ryoichi", "Soichi",
        "Taichi", "Yuichi", "Hidenori", "Masanori", "Toshinori", "Katsunori", "Akinori", "Nobuyuki",
        "Kazuyuki", "Tomoyuki", "Hiroyuki", "Toshiyuki", "Yasuyuki", "Shigeyuki", "Masayuki", "Nobuhiro",
        "Kazuhiro", "Tomohiro", "Yasuhiro", "Toshihiro", "Akihiro", "Norihiro", "Fumihiro", "Kentaro",
        "Taro", "Shiro", "Ken", "Jun", "Sen", "Dai", "Bin", "Jin",
        "Ran", "Shu", "Tao", "Retsu", "Kyo", "Nobunaga", "Ieyasu", "Yoshitsune",
        "Musashi", "Kojiro", "Sanosuke", "Heihachi", "Genzo", "Ginjiro", "Rikimaru", "Denjiro",
        "Kanjiro", "Seijiro", "Bunjiro", "Manjiro", "Unosuke", "Yajuro", "Tokujiro", "Umejiro",
        "Kumajiro", "Torajiro", "Fujitaro", "Ryuunosuke", "Shintaro", "Kotaro", "Rintaro", "Yutaka",
        "Gensai", "Heizo", "Kanzo", "Shozo", "Tatsuo", "Arata", "Sousuke", "Hayate",
        "Ryusei", "Souta", "Haruki", "Itsuki", "Kanata", "Hinata", "Asahi", "Subaru",
        "Kouki", "Shion", "Yuma", "Rento", "Souma", "Touma", "Aoi", "Rui",
        "Kento", "Reo", "Ryoma", "Genki", "Kaisei", "Kenshin", "Sousei", "Tatsuki",
        "Yuuma", "Kouta", "Naoya", "Daichi", "Kaname", "Kotarou", "Masaru", "Nozomu",
        "Sadamu", "Takashi", "Yoshikazu", "Isao", "Kunio", "Michio", "Naohiro", "Rikuto",
        "Seiji", "Tsuyoshi", "Zenjiro", "Bunzo", "Chuuji", "Denkichi", "Eikichi", "Fukujiro",
        "Gonzo", "Hikojiro", "Ippei", "Jinnai", "Kanroku", "Ginnojo", "Hanbei", "Iemon",
        "Akitsugu", "Akimasa", "Akimori", "Akinaga", "Akinobu", "Akitada", "Akitane", "Akizane",
        "Arinaga", "Aritomo", "Chikafusa", "Chikahide", "Chikakage", "Chikamasa", "Chikanaga", "Chikanobu",
        "Chikatada", "Chikatsugu", "Chikayoshi", "Fujinaga", "Fujinobu", "Fujitada", "Fujitsugu", "Fujiyasu",
        "Harumasa", "Harunaga", "Harunobu", "Harutada", "Harutsugu", "Haruyoshi", "Hideaki", "Hideharu",
        "Hidehisa", "Hidekatsu", "Hidemasa", "Hidemori", "Hidenaga", "Hidenobu", "Hidetada", "Hidetsugu",
        "Kageaki", "Kagekatsu", "Kagemori", "Kagenobu", "Kagetada", "Kageyasu", "Kageyoshi", "Katsuhide",
        "Katsumasa", "Katsunaga", "Katsunobu", "Katsutada", "Katsuyori", "Kiyomasa", "Kiyonaga", "Kiyonobu",
        "Kiyotada", "Kiyotsugu", "Masahide", "Masakage", "Masakatsu", "Masamori", "Masanaga", "Masanobu",
        "Masatada", "Masatsugu", "Masayori", "Mochihide", "Mochinaga", "Mochitoyo", "Moriharu", "Morihide",
        "Morikage", "Morimasa", "Morinaga", "Morinobu", "Moritada", "Moritsugu", "Nagamasa", "Naganobu",
        "Nagatada", "Nobuhide", "Nobukatsu", "Nobumasa", "Nobumori", "Nobutada", "Nobutsuna", "Norimasa",
        "Norinaga", "Noritada", "Sadaharu", "Sadahide", "Sadakatsu", "Sadamasa", "Sadanaga", "Sadanobu",
        "Sadayoshi", "Tadamasa", "Tadanaga", "Tadanobu"
    )

    private val JAPONES_F = listOf(
        "Sakura", "Yui", "Hana", "Aoi", "Rin", "Mio", "Akane", "Yuki",
        "Michiko", "Emiko", "Kaori", "Natsumi", "Reiko", "Sayuri", "Tomoko", "Chiyo",
        "Hinata", "Izumi", "Kumiko", "Nozomi", "Haruka", "Yuka", "Ayumi", "Chika",
        "Emi", "Erika", "Fumiko", "Hisako", "Junko", "Kanako", "Keiko", "Kimiko",
        "Kyoko", "Mariko", "Masako", "Midori", "Mika", "Miki", "Miyuki", "Naoko",
        "Noriko", "Rie", "Ritsuko", "Ryoko", "Sachiko", "Setsuko", "Shizuka", "Sumiko",
        "Takako", "Tamiko", "Terumi", "Tomomi", "Wakana", "Yasuko", "Yayoi", "Yoko",
        "Yoshiko", "Yuko", "Ai", "Airi", "Ami", "Anzu", "Chihiro", "Eri",
        "Fuyuko", "Harumi", "Hiroko", "Hitomi", "Ikuko", "Kaede", "Kazue", "Kikue",
        "Kiyoko", "Kumi", "Machiko", "Maki", "Mari", "Mayu", "Megumi", "Miho",
        "Misaki", "Mitsuko", "Momoko", "Nanami", "Nana", "Nanako", "Rika", "Riko",
        "Sae", "Saki", "Shiho", "Suzu", "Tae", "Tsubaki", "Tsukiko", "Wakako",
        "Yoshie", "Ayano", "Chieko", "Fujiko", "Sakiko", "Yumeko", "Kotone", "Akemi",
        "Mizuki", "Yozora", "Hikari", "Asuka", "Rinka", "Koharu", "Manami", "Sumire",
        "Akari", "Ayaka", "Chiharu", "Chinatsu", "Chisato", "Emika", "Etsuko", "Fumika",
        "Hazuki", "Hikaru", "Hiromi", "Iori", "Kaoru", "Karin", "Kasumi", "Kazumi",
        "Kimika", "Konomi", "Kotoha", "Kurumi", "Madoka", "Mai", "Manaka", "Marina",
        "Miu", "Miyu", "Momo", "Nagisa", "Nanase", "Natsuki", "Reika", "Rena",
        "Rina", "Sana", "Sango", "Satsuki", "Shiori", "Sonoko", "Suzuka", "Towa",
        "Umeko", "Wakaba", "Yoshino", "Yuina", "Yuzuki", "Amane", "Chidori", "Ayame",
        "Botan", "Hagino", "Ibuki", "Junna", "Kaya", "Machi", "Otowa", "Renge",
        "Sakuya", "Tsubame", "Uzuki", "Wasa", "Yura", "Aiko", "Beniko", "Chikako",
        "Fusae", "Gin", "Haru", "Ise", "Kado", "Kie", "Man", "Nao",
        "Oyo", "Sen", "Take", "Ume", "Waka", "Yae", "Fusa", "Hide",
        "Kata", "Kiwa", "Mine", "Riki", "Sada", "Sei", "Tami", "Toshi",
        "Aki", "Asa", "Ayae", "Chiyoha", "Etsu", "Fude", "Fuyu", "Hatsu",
        "Hisa", "Ichi", "Ine", "Iwa", "Kame", "Kane", "Kinu", "Kiyo",
        "Koto", "Kuma", "Masa", "Matsu", "Miya", "Miyo", "Moto", "Naka",
        "Natsu", "Nobu", "Rinzu", "Saeha", "Sakiho", "Sato", "Shige", "Shima",
        "Shino", "Sumi", "Taka", "Tama", "Tane", "Toki", "Tome", "Tora",
        "Toyo", "Tsune", "Yasu", "Yoshi", "Yukiha", "Atsu", "Chizu", "Fumi",
        "Hama", "Hanae", "Haruho", "Hisae", "Iku", "Ito", "Kiku", "Kimi",
        "Kinuha", "Kiyoha", "Koma", "Kotoe", "Masae", "Matsuha", "Mina", "Mitsu",
        "Miyoha", "Motoe", "Nami", "Nobuha", "Sadae", "Sakae", "Shigeha", "Shizu",
        "Suma", "Takae", "Tamae", "Taneha", "Tokiwa", "Tomeha", "Torae", "Tsuneko",
        "Uta", "Yaeha", "Yasuha", "Yoshiha", "Akiha", "Chikae", "Fuyue", "Hatsue",
        "Hisayo", "Iwae", "Kameyo", "Kaneyo", "Kinue", "Kiyoe", "Natsue", "Sumiye",
        "Toyoha", "Yoshieha", "Tsuya", "Sayo"
    )

    private val CHINES_M = listOf(
        "Wei", "Jian", "Hao", "Yong", "Feng", "Liang", "Chen", "Ming",
        "Qiang", "Bo", "Zhi", "Tao", "Yun", "Xin", "Kai", "Jun",
        "Long", "Sheng", "Rui", "Han", "Chao", "Cheng", "Dong", "Fei",
        "Gang", "Guang", "Hai", "Heng", "Hong", "Hui", "Jia", "Jie",
        "Jin", "Jing", "Kang", "Lei", "Liwei", "Peng", "Ping", "Qing",
        "Quan", "Shan", "Song", "Tian", "Wen", "Xiang", "Xiaobo", "Xiaoming",
        "Xu", "Yang", "Yi", "Yifan", "Yiming", "Yu", "Zhen", "Zhihao",
        "Zhong", "Bao", "Bin", "Cai", "Chang", "Chun", "De", "Fang",
        "Fu", "Gong", "Guo", "Hua", "Jinhai", "Kun", "Lin", "Min",
        "Nan", "Ning", "Qi", "Rong", "Ruilong", "Shui", "Tai", "Wang",
        "Wu", "Xian", "Xing", "Yan", "Yao", "Ye", "Zhao", "Zheng",
        "Zhiqiang", "Zhu", "Cong", "Dawei", "Fenghua", "Haoran", "Junjie", "Kaiwen",
        "Zihao", "Weilin", "Boyan", "Chengyu", "Ding", "Guangming", "Hengyu", "Jiayang",
        "Kunhao", "Longwei", "Minghao", "Renjie", "Shaoqing", "Tianyu", "Weilong", "Xiaofeng",
        "Yanbo", "Zhicheng", "Anbo", "Baoguo", "Chuang", "Fenglin", "Guohua", "Yunfeng",
        "Zeyu", "Boyu", "Caiwen", "Chengfeng", "Dequan", "Enze", "Fuhai", "Genghui",
        "Haocheng", "Jinlong", "Kaicheng", "Liren", "Mingzhe", "Nianzu", "Peizhi", "Qifeng",
        "Ruihao", "Shaowei", "Tengyu", "Weiming", "Xingye", "Yongkang", "Zhenbo", "Anlong",
        "Baisheng", "Chengrui", "Duanmu", "Fengchao", "Guangyu", "Haotian", "Jianguo", "Kaiyang",
        "Lianjun", "Muyang", "Ningyu", "Pengcheng", "Qingshan", "Rongguang", "Shuhao", "Tianze",
        "Wanshou", "Xichen", "Yiyang", "Zhongwei", "Aoshi", "Bingwen", "Chenglong", "Dongsheng",
        "Enlin", "Fengshan", "Guiying", "Haipeng", "Jianbo", "Lifeng", "Maoxing", "Naiwen",
        "Ouyang", "Pingan", "Qixin", "Rongxi", "Shuncheng", "Taiping", "Wangchuan", "Xingchen",
        "Yuncheng", "Zhaohui", "Anguo", "Baolong", "Chuankang", "Deheng", "Erkang", "Faqiang",
        "Guolin", "Hesheng", "Jinyi", "Kangwei", "Lianfa", "Mingqi", "Zixuan", "Zimo",
        "Zichen", "Zhenyu", "Zhiyuan", "Zhonglin", "Zhuoran", "Yichen", "Yifeng", "Yunfei",
        "Yuze", "Yuxuan", "Yongrui", "Xingyu", "Xuanming", "Wenhao", "Wenlong", "Weiran",
        "Shenghao", "Ruichen", "Qingyuan", "Mingxuan", "Mingyuan", "Jingyuan", "Haoyu", "Hongyi",
        "Haochen", "Guangyao", "Fengyu", "Chenxi", "Changfeng", "Bohan", "Anran", "Zhaolin",
        "Yuanhao", "Yunhao", "Xinghan", "Wenxuan", "Wenyu", "Rongxuan", "Ruiyang", "Qinghe",
        "Mingze", "Jinghao", "Jinze", "Hanyu", "Haoxuan", "Chenghao", "Baiyu", "Anzhi"
    )

    private val CHINES_F = listOf(
        "Mei", "Ling", "Xia", "Yan", "Fang", "Jing", "Li", "Hui",
        "Xiu", "Qing", "Yue", "Zhen", "Lan", "Ning", "Rou", "Shu",
        "Wen", "Xue", "Yin", "Zhu", "Bao", "Chun", "Yuan", "Dan",
        "Fen", "Feng", "Hua", "Huan", "Jia", "Jiao", "Jie", "Jin",
        "Juan", "Lian", "Liu", "Min", "Ping", "Qian", "Qiao", "Rui",
        "Shan", "Ting", "Wan", "Xiang", "Xiaohong", "Xiaoli", "Xiaoyan", "Xin",
        "Ya", "Yang", "Yao", "Yi", "Ying", "Yun", "Zhao", "Zhi",
        "Ai", "Bing", "Chen", "Cong", "Enhui", "Fenfen", "Guo", "Han",
        "Hong", "Huifen", "Jiahui", "Jiayi", "Jinhua", "Lanfen", "Lifang", "Lihua",
        "Meihui", "Meiling", "Ningning", "Peiyi", "Qiaoyan", "Ruiqi", "Ruoxi", "Shuang",
        "Suyin", "Tingting", "Wanling", "Xiaohua", "Xiaoyu", "Yanling", "Yanyu", "Yingying",
        "Yiyi", "Yuanyuan", "Yumei", "Zhilan", "Xiaofang", "Yalan", "Zhixin", "Peizhi",
        "Ruolan", "Simin", "Wenjuan", "Yanmei", "Baozhen", "Caixia", "Duoduo", "Enni",
        "Fangzhou", "Guixia", "Hanyu", "Jiaxin", "Lianhua", "Meixiang", "Peiling", "Qiuyue",
        "Ruyi", "Shuyi", "Wanqing", "Xinyi", "Yaqin", "Ailing", "Baozhu", "Anqi",
        "Baoyu", "Chunhua", "Duoxi", "Ehuang", "Fenglian", "Huiying", "Jinxiu", "Liling",
        "Meiqi", "Nuoyan", "Piaoxue", "Qingzhao", "Ranran", "Shuangyu", "Ting'er", "Wanru",
        "Xueyan", "Yingxue", "Zhiruo", "Aiyun", "Beilan", "Chunyan", "Danyu", "Enqi",
        "Fangru", "Guiqin", "Huifang", "Jiayun", "Kaili", "Lanxin", "Meilan", "Niya",
        "Ouyun", "Peiyu", "Qingying", "Rongxi", "Shuangqing", "Tingyu", "Wanxi", "Xiuqing",
        "Yalin", "Zhiying", "Aichun", "Baiyu", "Chunfeng", "Dongmei", "Enru", "Guimei",
        "Huilan", "Jinyu", "Lifen", "Mingzhu", "Ningxi", "Peiqing", "Qiuxiang", "Rulan",
        "Suqin", "Tingyun", "Wanyi", "Xiuhua", "Yulan", "Zhaoying", "Baixue", "Chenxi",
        "Duoyu", "Enxi", "Fengying", "Guolan", "Huiqing", "Jiaoyang", "Kailin", "Lirong",
        "Mengyao", "Niannian", "Ouxi", "Peihua", "Qianqian", "Ruixi", "Shiyu", "Zixuan",
        "Zihan", "Zhiyao", "Yunxi", "Yuxin", "Yueyao", "Yuexin", "Xinyue", "Tinglan",
        "Shuying", "Shuxian", "Qinglan", "Qingya", "Mingyue", "Lingxi", "Lanying", "Jingyi",
        "Jingxuan", "Hanyue", "Fanghua", "Biyun", "Baihe", "Yunshang", "Yunluo", "Yuehua",
        "Xueying", "Wenxin", "Shuyue", "Ronghua", "Qingyue", "Mingxia", "Meiying", "Jinglan",
        "Jinling", "Huaying", "Fengyi", "Chunlan", "Caiyun", "Biyue"
    )

    private val COREANO_M = listOf(
        "Minjun", "Seojun", "Junseo", "Yejun", "Hajun", "Doyun", "Siwoo", "Joowon",
        "Yujun", "Junwoo", "Jiho", "Woojin", "Hyunwoo", "Jaehyun", "Sungmin", "Donghyun",
        "Minseok", "Kyungsoo", "Taeyang", "Jonghyun", "Inho", "Sangmin", "Yongjun", "Chanho",
        "Daehyun", "Geonwoo", "Hyunjun", "Ilhwan", "Jaewon", "Kangmin", "Kyuwon", "Minwoo",
        "Namgoong", "Ohseung", "Pilseung", "Raewon", "Sanghyun", "Taewoo", "Ukjin", "Wonwoo",
        "Yeongjin", "Hojin", "Beomseok", "Changwook", "Dongwoo", "Euijae", "Geunhyung", "Hansol",
        "Hyungsik", "Ilsung", "Jaeyong", "Kangdae", "Kitae", "Manho", "Myeonghyun", "Namjin",
        "Ohjin", "Pyohyun", "Ryujin", "Sejin", "Sungjae", "Taehoon", "Uiseok", "Wonshik",
        "Yeonjae", "Yongho", "Bohyun", "Changhyun", "Dalsoo", "Eunjae", "Gyuhyun", "Hoseok",
        "Iljoo", "Jaehun", "Kyungjun", "Minsuk", "Namwoo", "Oktae", "Pyungho", "Ryuwon",
        "Sangchul", "Taeshik", "Uijin", "Wontae", "Yeonho", "Beomjun", "Chunwoo", "Daewoo",
        "Eunsuk", "Gwangjin", "Hosung", "Ilwoo", "Jaesuk", "Kyutae", "Manjae", "Namhyun",
        "Ohjun", "Piljae", "Ryumin", "Sangil", "Taejun", "Byungchul", "Doohan", "Gunwoo",
        "Haejin", "Jonghwan", "Kanginho", "Doyoon", "Sungjun", "Hyunbin", "Jaewoo", "Junho",
        "Kanghyun", "Namgyu", "Onyu", "Pyoungjun", "Sanghoon", "Taehyung", "Uisung", "Baekhyun",
        "Chungho", "Daehan", "Eunkyu", "Geumsu", "Hyeongsik", "Ilgook", "Jonghan", "Kyungmin",
        "Munsik", "Namgil", "Okhwan", "Pilsung", "Sechan", "Taemin", "Ungyu", "Wonpil",
        "Beomsu", "Chunsik", "Dongchul", "Eungyu", "Ganghan", "Hakjun", "Iksu", "Jaebum",
        "Kyungtae", "Manseok", "Namkyu", "Okgyu", "Pyeongho", "Rakjin", "Sangwoo", "Taeseok",
        "Wonho", "Yeongsu", "Baekseok", "Chulsoo", "Daeyoung", "Eungtae", "Gilsoo", "Hyunmin",
        "Jeongmin", "Kwanghee", "Munho", "Okjin", "Pilnam", "Sooho", "Yongsoo", "Yeongho",
        "Areumdari", "Chorong", "Dalsung", "Gaon", "Hanul", "Iseul", "Juhyuk", "Kyeol",
        "Maru", "Narae", "Ohan", "Pyeonghwa", "Raon", "Seongjae", "Taemu", "Uram",
        "Woobin", "Yiseo", "Bada", "Chan", "Daon", "Eumsung", "Garam", "Haneul",
        "Do-yun", "Ha-jun", "Seo-jun", "Si-woo", "Ye-jun", "Ji-ho", "Min-jun", "Hyun-woo",
        "Jun-seo", "Woo-jin", "Tae-yang", "Seong-hyun", "Jae-hyun", "Dong-hyun", "Gun-woo", "Hyun-jun",
        "Ji-hwan", "Jun-ho", "Seung-hyun", "Tae-hyun", "Woo-sung", "Young-ho", "Jin-woo", "Min-ho",
        "Sang-hyun", "Seung-ho", "Tae-jun", "Joon-ho", "Hae-jin", "Kyung-ho", "Dae-hyun", "Dong-ha",
        "Jae-won", "Seon-woo", "Jun-young", "Hyun-seok", "Min-seok", "Sung-ho", "Tae-woo", "Won-ho"
    )
    private val COREANO_F = listOf(
        "Jiwoo", "Seoyeon", "Haeun", "Yeeun", "Sooah", "Minseo", "Chaewon", "Daeun",
        "Eunji", "Gaeun", "Hayoon", "Inah", "Jimin", "Kyunghee", "Mirae", "Naeun",
        "Okhee", "Yejin", "Sunhee", "Yeonhee", "Boram", "Chaerin", "Dasom", "Eunhye",
        "Gayoung", "Harin", "Insuk", "Jiyeon", "Kyungah", "Misun", "Narae", "Okja",
        "Yerin", "Sunyoung", "Yeonju", "Bomi", "Chaeeun", "Dajung", "Eunkyung", "Garin",
        "Haneul", "Inyeong", "Jieun", "Kyungmi", "Miyoung", "Nayoon", "Oksun", "Yewon",
        "Sunmi", "Yeonwoo", "Boyoung", "Chaeyoung", "Dahye", "Eunsook", "Gahyun", "Hayoung",
        "Injung", "Jihye", "Kyungja", "Mihwa", "Nakyung", "Okyeon", "Yeseul", "Sunja",
        "Yeonsook", "Bokyung", "Chaeyeon", "Eunmi", "Gain", "Inhye", "Jisoo", "Kyungeun",
        "Miok", "Nahee", "Okhwa", "Sunhwa", "Yeonjin", "Bohee", "Chaeryung", "Dabin",
        "Eunseo", "Gayeon", "Halin", "Inseon", "Kyunghwa", "Mijin", "Nayoung", "Okryun",
        "Yelim", "Sunkyung", "Yeonhwa", "Bora", "Chaeah", "Dajin", "Eunbin", "Gabin",
        "Hasom", "Inkyung", "Jia", "Yerim", "Somin", "Yuna", "Areum", "Bitna",
        "Chorong", "Danbi", "Eunha", "Hyerin", "Iseul", "Jungeun", "Kkotnim", "Mikyung",
        "Naeri", "Pureum", "Rina", "Saebom", "Taeyeon", "Uikyung", "Wooyoung", "Yebin",
        "Chaerim", "Damhee", "Eunsol", "Garam", "Hyewon", "Jinkyung", "Kaeun", "Miso",
        "Nabi", "Odam", "Rumi", "Sena", "Yena", "Chunja", "Eunju", "Gwija",
        "Haeja", "Insook", "Jeonghee", "Kumja", "Miran", "Nari", "Pilja", "Ranhee",
        "Sooja", "Taehee", "Unhui", "Wolhee", "Yonghee", "Boae", "Chunhwa", "Dahae",
        "Geumhui", "Hyoja", "Ilhui", "Jinja", "Malja", "Oksoon", "Pilhwa", "Ryeowon",
        "Taeyoung", "Wonja", "Yeonja", "Dalsom", "Gaeul", "Hanbit", "Juyeon", "Kyerim",
        "Maru", "Onnuri", "Pureumi", "Raeum", "Sarang", "Taeun", "Uram", "Yiseul",
        "Bada", "Charae", "Eumbo", "Isaeng", "Jiseo", "Kyeol", "Miru", "Nunkkot",
        "Yeowon", "Seoah", "Dain", "Suhyun", "Nayeon", "Doyeon", "Yuri", "Seo-yeon",
        "Seo-yun", "Ji-woo", "Ha-yun", "Min-seo", "Ji-yoo", "Soo-ah", "Ye-eun", "Da-eun",
        "Eun-seo", "Hye-jin", "Soo-jin", "Min-ji", "Ji-hye", "Eun-ji", "Yeon-woo", "Seol-ah",
        "Ha-eun", "Na-eun", "Yu-na", "Bo-ra", "Ga-eun", "Hae-won", "Jin-ah", "Kyung-mi",
        "Mi-ra", "Seon-hee", "Soo-yeon", "Yeon-hwa", "Eun-bi", "Hye-won", "Ji-eun", "Min-ah",
        "Seo-hyeon", "Soo-bin", "Ye-rin", "Yu-ri", "Hye-rin", "Da-in", "Ga-young"
    )

    private val ISLANDES_M = listOf(
        "Hrafn", "Úlfur", "Ari", "Bersi", "Bjarni", "Brandur", "Dagur", "Einar",
        "Eiríkur", "Eldjárn", "Eyjólfur", "Finnur", "Flóki", "Frosti", "Geir", "Gísli",
        "Gunnar", "Haukur", "Hjalti", "Hjörvar", "Hrólfur", "Ingólfur", "Ketill", "Kári",
        "Leifur", "Njáll", "Orri", "Ragnar", "Rúnar", "Snorri", "Styrmir", "Svanur",
        "Tryggvi", "Vésteinn", "Vigfús", "Þórir", "Þorsteinn", "Örn", "Agnar", "Arnar",
        "Árni", "Ásbjörn", "Ásgeir", "Ásmundur", "Bjarki", "Bjartur", "Björn", "Brynjar",
        "Egill", "Erlendur", "Erlingur", "Eyvindur", "Hákon", "Hálfdan", "Hallbjörn", "Helgi",
        "Hjálmar", "Indriði", "Ingi", "Ingvar", "Sigmundur", "Sturla", "Sveinn", "Sverrir",
        "Þórarinn", "Þorbjörn", "Þorgrímur", "Þórður", "Birkir", "Breki", "Fannar"
    )

    private val ISLANDES_F = listOf(
        "Björk", "Hekla", "Embla", "Sigrún", "Álfheiður", "Arndís", "Ása", "Auður",
        "Bergþóra", "Brynhildur", "Dagbjört", "Dís", "Edda", "Fjóla", "Gróa", "Guðrún",
        "Hallbera", "Hildur", "Hrefna", "Hulda", "Ingibjörg", "Katla", "Kolbrún", "Laufey",
        "Mist", "Ragnheiður", "Rúna", "Sól", "Svanhildur", "Unnur", "Vigdís", "Yrsa",
        "Þórdís", "Þórunn", "Arna", "Arnbjörg", "Ásdís", "Ásgerður", "Áslaug", "Ásta",
        "Ástríður", "Aðalbjörg", "Björg", "Borghildur", "Brynja", "Dagný", "Dagrún", "Elfa",
        "Erla", "Gyða", "Hafdís", "Halla", "Hildigunnur", "Hjördís", "Hlíf", "Ingunn",
        "Sigríður", "Signý", "Svana", "Þóra", "Þorbjörg", "Þórhildur", "Alda"
    )

    private val IRLANDES_M = listOf(
        "Tadhg", "Cian", "Fionn", "Oisín", "Cormac", "Niall", "Conall", "Conchobhar",
        "Diarmaid", "Donncha", "Cathal", "Aodh", "Ailill", "Art", "Bran", "Cairbre",
        "Caoimhín", "Colmán", "Crimthann", "Dáire", "Dónal", "Éamon", "Eoghan", "Faolán",
        "Fearghal", "Fiachra", "Lorcán", "Naoise", "Odhrán", "Rónán", "Ruairí", "Séaghdha",
        "Senán", "Tuathal", "Aengus", "Ailbhe", "Aodhagán", "Breandán", "Brian", "Caolán",
        "Cearbhall", "Cillian", "Coinneach", "Conaire", "Conn", "Conrí", "Dáithí", "Dallán",
        "Donn", "Donnán", "Dubhán", "Éanna", "Eochaid", "Fachtna", "Ferdomnach", "Fergal",
        "Fiach", "Fionnbharr", "Flann", "Guaire", "Laoise", "Murchadh", "Nechtan", "Ríoghán",
        "Rossa", "Ruaidhrí", "Suibhne"
    )

    private val IRLANDES_F = listOf(
        "Aoife", "Siobhán", "Niamh", "Caoimhe", "Saoirse", "Gráinne", "Áine", "Aisling",
        "Bébinn", "Bláthnaid", "Brídín", "Caoilfhionn", "Clíodhna", "Deirdre", "Éabha", "Éadaoin",
        "Étaín", "Fionnuala", "Lasairfhíona", "Líadan", "Maeve", "Meadhbh", "Móirín", "Muireann",
        "Neasa", "Nessa", "Órla", "Róisín", "Sadhbh", "Síle", "Sorcha", "Úna",
        "Aifric", "Aoibhe", "Aoibhinn", "Béibhinn", "Brónach", "Caoilinn", "Cessair", "Ciara",
        "Clodagh", "Dearbháil", "Doireann", "Eithne", "Emer", "Fainche", "Fodhla", "Gormlaith",
        "Laoise", "Macha", "Méadbh", "Mór", "Nuala", "Ríona", "Ríoghnach", "Saorla",
        "Síomha", "Treasa"
    )

    private val HUNGARO_M = listOf(
        "Álmos", "Árpád", "Ákos", "Botond", "Bulcsú", "Csaba", "Csanád", "Előd",
        "Huba", "Lehel", "Levente", "Ond", "Tas", "Taksony", "Töhötöm", "Zoltán",
        "Bendegúz", "Keve", "Örs", "Szabolcs", "Vajk", "Koppány", "Tarhos", "Üllő",
        "Solt", "Farkas", "Gyula", "Jenő", "Kende", "Kurszán", "Csongor", "Elemér",
        "Géza", "Kund", "Lél", "Sur", "Tétény", "Zalán", "Zente", "Zétény",
        "Zsombor", "Bors", "Buda", "Kadosa", "Karcsa", "Kond", "Kölcse", "Magor",
        "Örsúr", "Soma", "Tarcal", "Torda", "Vata", "Zobor"
    )

    private val HUNGARO_F = listOf(
        "Emese", "Réka", "Enikő", "Tünde", "Bíborka", "Boglárka", "Aranka", "Emőke",
        "Ildikó", "Hajna", "Sarolt", "Csenge", "Dalma", "Gyöngyvér", "Hanga", "Piroska",
        "Tímea", "Virág", "Zselyke", "Ajándék", "Arany", "Csillag", "Déva", "Enéh",
        "Lelle", "Nyesta", "Szépa", "Csilla", "Gyöngyi", "Hajnal", "Hajnalka", "Ilka",
        "Kincső", "Panna", "Tícia", "Tündér", "Zenge", "Zorka", "Borcsa", "Csobánka",
        "Gyöngy", "Hajnácska", "Harmat", "Kendea", "Pintyőke", "Sugár", "Szellő", "Tavirózsa",
        "Tűzvirág"
    )

    private val AFRICANO_M = listOf(
        "Kwame", "Sekou", "Themba", "Jabari", "Kofi", "Obi", "Chidi", "Amadi",
        "Tau", "Sipho", "Bakari", "Kaya", "Mensah", "Njoroge", "Olamide", "Rashidi",
        "Tendai", "Zuberi", "Emeka", "Abioye", "Babajide", "Chike", "Dayo", "Ekon",
        "Fola", "Gyasi", "Ige", "Jelani", "Kamau", "Lekan", "Mandla", "Ngozi",
        "Oba", "Paki", "Quame", "Ronke", "Simba", "Tafari", "Uzoma", "Vusi",
        "Wole", "Zola", "Ade", "Bello", "Chukwuma", "Diallo", "Farai", "Ike",
        "Jomo", "Kagiso", "Lumumba", "Mosi", "Ndulu", "Oluwaseun", "Panya", "Shaka",
        "Taye", "Vuyani", "Wafula", "Xolani", "Yaw", "Zawadi", "Abebe", "Chinedu",
        "Diakite", "Femi", "Ifeanyi", "Jabulani", "Kojo", "Lwazi", "Nnamdi", "Onyeka",
        "Somto", "Tunde", "Uche", "Wanjala", "Yohannes", "Zenzo", "Adisa", "Boakai",
        "Dembo", "Folu", "Gomba", "Kwabena", "Kwaku", "Ato", "Kweku", "Ekow",
        "Kobina", "Chima", "Ikenna", "Kelechi", "Obinna", "Okechukwu", "Ayo", "Bamidele",
        "Babatunde", "Kayode", "Olumide", "Seyi", "Adebowale", "Folami", "Kato", "Thabo",
        "Sizwe", "Bongani", "Lethabo", "Kabelo", "Tshepo", "Kgosi", "Mpho", "Temba"
    )

    private val AFRICANO_F = listOf(
        "Amara", "Zola", "Chiamaka", "Folasade", "Imani", "Kesi", "Makena", "Nia",
        "Oni", "Sanaa", "Thandiwe", "Wanjiru", "Yejide", "Zaria", "Adaeze", "Bisa",
        "Chinara", "Eshe", "Kamaria", "Abeni", "Bayo", "Chidinma", "Dara", "Ekwutosi",
        "Fumnanya", "Gimbya", "Halima", "Ijeoma", "Jendayi", "Mariama", "Oyin", "Rehema",
        "Sade", "Temi", "Ummi", "Vuyokazi", "Wangari", "Xola", "Yaa", "Adaora",
        "Bontle", "Chinwe", "Doreen", "Eniola", "Fisayo", "Hadiya", "Ifeoma", "Jumoke",
        "Kadiatu", "Lulu", "Mbali", "Nkechi", "Ozioma", "Pumla", "Rahma", "Shani",
        "Titi", "Ubax", "Vimbai", "Winnie", "Yetunde", "Zuri", "Bulelwa", "Chiedza",
        "Dalili", "Ebele", "Feyi", "Gugulethu", "Iyabo", "Kembo", "Lerato", "Mumbi",
        "Nala", "Onyinye", "Palesa", "Rumbidzai", "Sitembile", "Tariro", "Uwimana", "Vongai",
        "Wema", "Yamikani", "Abisola", "Chiaka", "Ndidi", "Amadika", "Bosede", "Chinyere",
        "Ayanna", "Abena", "Adwoa", "Akosua", "Ama", "Esi", "Afia", "Aba",
        "Efua", "Araba", "Amaka", "Ngozi", "Nneka", "Obiageli", "Uchechi", "Ayana",
        "Naledi", "Nomsa", "Nandi", "Lindiwe", "Busisiwe", "Zanele", "Ayodele", "Folami",
        "Morayo", "Adesina", "Malaika", "Neema", "Penda", "Safiya", "Taji", "Zawadi",
        "Asha", "Nyah"
    )



    private val ARABE_M = listOf(
        "Amir", "Bashir", "Farid", "Hakim", "Jamal", "Karim", "Malik", "Nasir",
        "Qadir", "Rashid", "Sami", "Tariq", "Adil", "Fahad", "Anwar", "Basil",
        "Fadi", "Ghassan", "Habib", "Imad", "Jawad", "Khalid", "Latif", "Nabil",
        "Qasim", "Rami", "Salim", "Tarek", "Wasim", "Yasser", "Adnan", "Firas",
        "Hisham", "Jaber", "Kareem", "Marwan", "Nadir", "Rashad", "Saeed", "Sultan",
        "Waleed", "Younes", "Aziz", "Bashar", "Farouk", "Hatem", "Majid", "Nasser",
        "Rafiq", "Saad", "Talal", "Wahid", "Bader", "Fayez", "Ghazi", "Haitham",
        "Iyad", "Jaser", "Khalil", "Mounir", "Nazir", "Rida", "Sabri", "Tahir",
        "Wael", "Ziad", "Akram", "Bassam", "Emad", "Faisal", "Hadi", "Jalal",
        "Khaled", "Mansour", "Nawaf", "Rafat", "Sadiq", "Taymour", "Walid", "Suhail",
        "Firdaus", "Ammar", "Aref", "Badr", "Bahram", "Faris", "Fawaz", "Haidar",
        "Harith", "Hazim", "Hilal", "Kamal", "Kinan", "Laith", "Luay", "Mazin",
        "Mudar", "Mundhir", "Nadim", "Nizar", "Qays", "Rakan", "Rayyan", "Saif",
        "Samer", "Sinan", "Zahir", "Ziyad", "Zuhayr", "Amr", "Aws", "Hani",
        "Jasim", "Khaldun", "Maan", "Nawfal", "Qutaiba", "Suhayb"
    )

    private val ARABE_F = listOf(
        "Amira", "Farida", "Hana", "Jamila", "Karima", "Layla", "Nadia", "Rania",
        "Samira", "Yasmin", "Zahra", "Dalia", "Leila", "Nour", "Rima", "Salma",
        "Widad", "Amal", "Basma", "Dunya", "Farah", "Ghada", "Hanan", "Iman",
        "Jumana", "Lubna", "Maha", "Nawal", "Rasha", "Sahar", "Thana", "Warda",
        "Yara", "Alia", "Bushra", "Dima", "Elham", "Fadwa", "Ghina", "Hala",
        "Inaya", "Jana", "Khawla", "Lamia", "Manal", "Rawan", "Sana", "Taghrid",
        "Wafaa", "Yumna", "Zahida", "Abir", "Batul", "Duha", "Eman", "Ghazala",
        "Hind", "Ikram", "Jawahir", "Kawthar", "Lina", "Najla", "Rahaf", "Sawsan",
        "Tasneem", "Wisal", "Yusra", "Zara", "Aziza", "Bahia", "Dalal", "Enas",
        "Ghalia", "Hayat", "Jihan", "Kamila", "Lujain", "Marwa", "Najat", "Raghad",
        "Sabah", "Thuraya", "Wardah", "Zaina", "Amani", "Basira", "Dahab", "Sabreen",
        "Wafiya", "Yasira", "Muna", "Abeer", "Afnan", "Anoud", "Arwa", "Asmahan",
        "Huda", "Inas", "Lama", "Lamis", "Maysa", "Munira", "Nahla", "Nisreen",
        "Rana", "Reem", "Shatha", "Suha", "Zubaida", "Buthaina", "Layal", "Mais",
        "Ruba"
    )

    private val MESOAMERICANO_M = listOf(
        "Acatl", "Balam", "Chimalli", "Cuauhtli", "Itzcoatl", "Nezahual", "Ocelotl", "Quetzal",
        "Tecolotl", "Tepin", "Xocoyotl", "Yaotl", "Ek", "Kabil", "Nachi", "Tzotz",
        "Kin", "Balche", "Cipactli", "Huitzilin", "Itztli", "Malinal", "Nahui", "Ollin",
        "Pakal", "Quiahuitl", "Tenoch", "Tepiltzin", "Toltecatl", "Tzimtzum", "Xicohtencatl", "Xiuhcoatl",
        "Yohualli", "Zolin", "Acamapichtli", "Atototl", "Cuetzpalin", "Ehecatzin", "Huemac", "Kanek",
        "Miquiztli", "Nezahualcoyotl", "Ocotlan", "Pakalotl", "Quauhtemoc", "Tecpatl", "Teponaztli", "Tlacaelel",
        "Tochtli", "Yaomahuitl", "Ahmakiq", "Balamku", "Chak", "Kukulkanil", "Nachan", "Ozomatli",
        "Popoca", "Tepetl", "Tochtin", "Uicab", "Yaxche", "Bahlam", "Chan", "Kan",
        "Junajpu", "Nikte", "Sak", "Yax", "Kuk", "Mo", "Ahkin", "Cabnal",
        "Coyotl", "Tlacoyotl", "Ozomahtli", "Cuitlahuac", "Moctezuma", "Xicotencatl", "Coyopil", "Ahuitzotl",
        "Acolmiztli", "Ahuizotl", "Axayacatl", "Cacamatzin", "Chimalpopoca", "Cuauhtemoc", "Cuauhtleco", "Ixtlilxochitl",
        "Maxtla", "Nezahualpilli", "Opochtli", "Tecuichpo", "Tezozomoc", "Tizoc", "Tlahuicole", "Huitzilihuitl",
        "Matlal", "Totoquihuatzin", "Xihuitl"
    )

    private val MESOAMERICANO_F = listOf(
        "Citlali", "Malinalli", "Papan", "Quetzalxochitl", "Tenoch", "Xochitl", "Yaretzi", "Zyanya",
        "Atl", "Itotia", "Nenetl", "Sac", "Nicte", "Teicui", "Zaniyah", "Ameyalli",
        "Chalchiuitl", "Ehecatzin", "Ichtaca", "Itzel", "Kinuani", "Malinalxochitl", "Necahual", "Ozelotl",
        "Papatzin", "Quiauhxochitl", "Sihuatl", "Tepin", "Tlacoehua", "Toyaotzin", "Xiuhtonal", "Yollotl",
        "Zolina", "Ixquic", "Kuk", "Ozomahtli", "Pakal", "Quetzalpetlatl", "Been", "Tayahui",
        "Tlanextli", "Uixtoti", "Yaocihuatl", "Zaachi", "Chimalma", "Elotl", "Huitzilxochitl", "Itzayana",
        "Kimuak", "Malinche", "Nikte", "Xic", "Pakalxoch", "Quetzalcue", "Sasil", "Tepiltzin",
        "Uey", "Xelha", "Yaxche", "Zuhuy", "Chel", "Ehecacihuatl", "Nahuiollin", "Ozelocihuatl",
        "Papaloxochitl", "Quiahuitzin", "Sak", "Kab", "Uacalxochitl", "Xicalli", "Yaxkin", "Zuwan",
        "Ahotl", "Elzabal", "Huemac", "Xilotzin", "Chalchiuhitl", "Toztli", "Necuametl", "Atotoztli",
        "Chalchiuhnenetzin", "Tecuelhuetzin", "Tecuichpo", "Xiuhtlaltzin", "Izel", "Metztli", "Quetzalli", "Eloxochitl",
        "Icnoyotl", "Ihuicatl", "Ilhuitl", "Malinal", "Mecatl", "Miyaoaxochitl", "Nahui", "Nelli",
        "Tlalli", "Tonalli", "Xiloxoch", "Yolotli"
    )

    // Sobrenomes reais e de uso documentado. Mantidos separados dos prenomes para
    // ampliar o espaço combinatório sem duplicar nomes completos no código.

    private val SOBRENOMES_JAPONESA = listOf(
        "Sato", "Suzuki", "Tanaka", "Watanabe", "Takahashi", "Ito", "Yamamoto", "Nakamura",
        "Kobayashi", "Saito", "Kato", "Yoshida", "Yamada", "Sasaki", "Matsumoto", "Yamaguchi",
        "Inoue", "Kimura", "Shimizu", "Hayashi", "Abe", "Ono", "Mori", "Nakajima",
        "Hashimoto", "Ikeda", "Yamazaki", "Ishikawa", "Yamashita", "Ogawa", "Ishii", "Sakamoto",
        "Goto", "Maeda", "Okada", "Hasegawa", "Fujita", "Kondo", "Sakai", "Murakami",
        "Ota", "Kikuchi", "Ueda", "Arai", "Endo", "Aoki", "Takeuchi", "Nakano",
        "Fujii", "Fukuda", "Kaneko", "Nishimura", "Okamoto", "Nakagawa", "Fujiwara", "Miura",
        "Harada", "Matsuda", "Kojima", "Morita", "Tamura", "Nakayama", "Ishida", "Wada",
        "Shibata", "Masuda", "Hara", "Ando", "Uchida", "Shimada", "Yokoyama", "Miyamoto",
        "Honda", "Koyama", "Takagi", "Miyazaki", "Taniguchi", "Ueno", "Imai", "Maruyama",
        "Kudo", "Sugiyama", "Fujimoto", "Kawamura", "Kono", "Murata", "Kubota", "Hirano",
        "Otsuka", "Noguchi", "Takeda", "Matsui", "Sakurai", "Chiba", "Kubo", "Sugawara",
        "Kinoshita", "Takada", "Nomura", "Adachi", "Akagawa", "Akamine", "Akutagawa", "Amemiya",
        "Arakawa", "Arima", "Asano", "Ashikaga", "Azuma", "Baba", "Chinen", "Date",
        "Doi", "Eguchi", "Fukui", "Fukushima", "Furukawa", "Hamada", "Hase", "Hatakeyama",
        "Hattori", "Hayakawa", "Higashi", "Hiraga", "Hori", "Hosokawa", "Ichikawa", "Igarashi",
        "Ikegami", "Imagawa", "Inaba", "Ishibashi", "Ishiguro", "Ishihara", "Itoh", "Iwata",
        "Kagawa", "Kajiwara", "Kakizaki", "Kanemaru", "Kasahara", "Kataoka", "Kawaguchi", "Kawahara",
        "Kawashima", "Kitagawa", "Kitamura", "Kobayakawa", "Kodama", "Koizumi", "Komatsu", "Konishi",
        "Kuroda", "Maekawa", "Matsunaga", "Matsushita", "Matsuyama", "Mikami", "Minamoto", "Miyake",
        "Morioka", "Moriyama", "Nagano", "Naito", "Nakamoto", "Nishida", "Nishikawa", "Obata",
        "Ogata", "Okabe", "Okazaki", "Ouchi", "Oyama", "Sakakibara", "Sanada", "Shimazu",
        "Soma", "Sugihara", "Tachibana", "Takenaka", "Tani", "Tokugawa", "Tomita", "Uesugi",
        "Ukita", "Watanuki", "Yagyu", "Yamane", "Yanagisawa"
    )

    private val SOBRENOMES_CHINESA = listOf(
        "Wang", "Li", "Zhang", "Liu", "Chen", "Yang", "Huang", "Wu",
        "Xu", "Zhao", "Zhou", "Lu", "Zhu", "Sun", "He", "Ma",
        "Yu", "Hu", "Lin", "Jiang", "Guo", "Luo", "Gao", "Zheng",
        "Liang", "Tang", "Wei", "Shi", "Xie", "Song", "Han", "Deng",
        "Feng", "Cao", "Peng", "Zeng", "Xiao", "Tian", "Dong", "Pan",
        "Yuan", "Cai", "Du", "Ye", "Cheng", "Su", "Ding", "Ren",
        "Shen", "Yao", "Jin", "Tan", "Liao", "Fan", "Xiong", "Qin",
        "Qiu", "Hou", "Shao", "Meng", "Long", "Wan", "Duan", "Lei",
        "Qian", "Yin", "Yi", "Chang", "Qiao", "Lai", "Gong", "Wen",
        "Pang", "Lan", "Ni", "Ji", "Tong", "Bao", "Yan", "Zhuang",
        "Nie", "Mo", "Kong", "Xiang", "Jia", "Gu", "Pei", "Bai",
        "Cui", "Kang", "Mao", "Jing", "Jiao", "Bi", "Bian", "Cen",
        "Chai", "Che", "Chi", "Chu", "Cong", "Dai", "Diao", "Dou",
        "Fang", "Fei", "Fu", "Gan", "Ge", "Geng", "Guan", "Hao",
        "Hong", "Hua", "Huan", "Huo", "Jian", "Ke", "Kuang", "Ling",
        "Lou", "Mai", "Man", "Miao", "Min", "Mu", "Ning", "Ou",
        "Pi", "Ping", "Pu", "Qu", "Ran", "Rao", "Rong", "Sang",
        "Shan", "Shang", "She", "Sheng", "Shu", "Si", "Sui", "Tao",
        "Teng", "Tu", "Weng", "Xia", "Xing", "Yong", "You", "Zhai",
        "Zhan", "Zhong", "Zou"
    )

    private val SOBRENOMES_COREANA = listOf(
        "Kim", "Lee", "Park", "Jeong", "Choi", "Cho", "Kang", "Yoon",
        "Jang", "Lim", "Han", "Oh", "Seo", "Shin", "Kwon", "Hwang",
        "Ahn", "Song", "Hong", "Yang", "Son", "Ko", "Moon", "Bae",
        "Baek", "Heo", "Nam", "Shim", "Ha", "Kwak", "Cha", "Na",
        "Min", "Byun", "Won", "Bang", "Woo", "No", "Ji", "Jin",
        "Yoo", "Ryu", "Jeon", "Gong", "Im", "Seok", "Seol", "Ma",
        "Gil", "Yeom", "Sung", "Joo", "Gu", "Eom", "Chae", "Cheon",
        "Hyeon", "Ham", "Byeon", "Chu", "Do", "So", "Seon"
    )

    private val SOBRENOMES_AFRICANA = listOf(
        "Mensah", "Owusu", "Osei", "Boateng", "Appiah", "Asare", "Yeboah", "Tetteh",
        "Adjei", "Asante", "Opoku", "Addo", "Ofori", "Amoah", "Adu", "Antwi",
        "Asamoah", "Obeng", "Frimpong", "Boakye", "Musa", "Sani", "Garba", "Bello",
        "Haruna", "Lawal", "Okafor", "Okeke", "Eze", "Nwosu", "Nwankwo", "Balogun",
        "Adebayo", "Adeyemi", "Ogunleye", "Olawale", "Mokoena", "Nkosi", "Ndlovu", "Khumalo",
        "Dlamini", "Mthembu", "Zulu", "Mahlangu", "Mabena", "Molefe", "Motsepe", "Mwangi",
        "Kamau", "Njoroge", "Otieno", "Omondi", "Kiptoo", "Kiplagat", "Mutiso", "Wanjiku",
        "Bekele", "Tesfaye", "Kebede", "Alemu", "Abebe", "Tadesse", "Girma", "Diallo",
        "Traore", "Keita", "Coulibaly", "Camara", "Toure", "Kone", "Cisse", "Diop",
        "Ndiaye", "Fall", "Ba", "Sow", "Gueye", "Faye", "Mendy", "Jallow",
        "Sesay", "Koroma", "Kamara", "Conteh", "Bangura", "Dumbuya", "Kallon", "Turay",
        "Sankoh", "Acheampong", "Agyeman", "Akoto", "Amankwah", "Annan", "Ansah", "Awuah",
        "Badu", "Darko", "Donkor", "Essien", "Gyasi", "Kwarteng", "Manu", "Nyarko",
        "Prempeh", "Sarpong", "Achebe", "Anyanwu", "Chukwu", "Ekwueme", "Ibe", "Iroha",
        "Madueke", "Nwachukwu", "Nwafor", "Nweke", "Onwudiwe", "Umeh", "Afolayan", "Akinola",
        "Akindele", "Alabi", "Arowolo", "Awolowo", "Bamgbose", "Dada", "Fagbemi", "Fashola",
        "Ojo", "Olowu", "Omotoso", "Oni", "Oyelowo", "Salami", "Afolabi", "Masango",
        "Maseko", "Mkhize", "Mofokeng", "Motaung", "Mthethwa", "Ntuli", "Radebe", "Sibeko",
        "Sithole", "Tshabalala"
    )

    private val SOBRENOMES_ARABE = listOf(
        "Haddad", "Khalil", "Mansour", "Saliba", "Shaheen", "Matar", "Murad", "Assaf",
        "Jaber", "Hamdan", "Awad", "Khatib", "Najjar", "Karam", "Farhat", "Aboud",
        "Deeb", "Ayoub", "Salem", "Qasim", "Alyan", "Obeidat", "Khalayleh", "Rawashdeh",
        "Momani", "Hammad", "Salama", "Mousa", "Nasser", "Nassar", "Khoury", "Habib",
        "Kassis", "Sayegh", "Dagher", "Zein", "Hariri", "Rahman", "Mahfouz", "Masri",
        "Darwish", "Fakhoury", "Shami", "Tamimi", "Rifai", "Bakri", "Halabi", "Jarrar",
        "Qattan", "Saadi", "Zahran", "Kanaan", "Khalaf", "Rashed", "Amin", "Hakim",
        "Karim", "Latif", "Naim", "Samaha", "Sabbagh", "Tawil", "Wahba", "Zaki",
        "Zidan", "Zoubi", "Barakat", "Bishara", "Ghanem", "Makhlouf", "Nasr", "Rizk",
        "Saab", "Safi", "Said", "Samaan", "Younes", "Zoghbi", "Akl", "Azar",
        "Chahine", "Fares", "Gemayel", "Hobeika", "Maalouf", "Sarkis", "Yazbek", "Abaza",
        "Agha", "Ajami", "Alami", "Atrash", "Badran", "Baroud", "Bitar", "Dabbagh",
        "Daher", "Dandachi", "Fakhri", "Ghazal", "Hammoud", "Houri", "Jalal", "Kabbani",
        "Khazen", "Madi", "Mikati", "Nahas", "Qabbani", "Qasem", "Rahal", "Rihani",
        "Saadeh", "Safadi", "Shihab", "Shukri", "Skaff", "Tannous", "Tueni", "Yamani",
        "Yazigi"
    )

    private val SOBRENOMES_MESOAMERICANA = listOf(
        "Caal", "Choc", "Coc", "Pop", "Chub", "Tzul", "Batz", "Coyoy",
        "Cumes", "Otzoy", "Sipac", "Chavajay", "Sosof", "Reanda", "Canul", "Cux",
        "Yax", "Pacay", "Cojti", "Xiloj", "Sican", "Xicay", "Ajanel", "Tum",
        "Balam", "Tot", "Koj", "Canastuj", "Sanic", "Oscoy", "Can", "Abaj",
        "Chumil", "Cum", "Xiquin", "Ajxup", "Sacalxot", "Tisol", "Akabal", "Noj",
        "Morales", "Gomez", "Velasquez", "Mendez", "Mejia", "Aguilar", "Juarez", "Alvarado",
        "Herrera", "Ortiz", "Chavez", "Estrada", "Mendoza", "Gutierrez", "Guzman", "Jimenez",
        "Castro", "Monroy", "Pineda", "Barrios", "Rosales", "Rodas", "Rivera", "Contreras",
        "Ruiz", "Cifuentes", "Oliva", "Castellanos", "Giron", "Sandoval", "Salazar", "Orellana",
        "Soto", "Cabrera", "Fuentes", "Figueroa", "Samayoa", "Recinos", "Avila", "Barrera",
        "Palencia", "Aldana", "Sosa", "Ical", "Ixcol", "Ixmatá", "Macario", "Maxia",
        "Pac", "Quib", "Sac", "Taj", "Tepaz", "Toj", "Tzoc", "Tzunun",
        "Yac", "Yool"
    )

    private val SOBRENOMES_ISLANDESA_BASE = listOf(
        "Ari", "Bersi", "Bjarni", "Brandur", "Dagur", "Einar", "Eiríkur", "Eyjólfur",
        "Finnur", "Flóki", "Geir", "Gísli", "Gunnar", "Haukur", "Hjalti", "Hrafn",
        "Ingólfur", "Kári", "Ketill", "Leifur", "Njáll", "Orri", "Ragnar", "Rúnar",
        "Snorri", "Styrmir", "Svanur", "Tryggvi", "Úlfur", "Vésteinn", "Vigfús", "Þórir",
        "Þorsteinn", "Örn", "Agnar", "Arnar", "Árni", "Ásbjörn", "Ásgeir", "Ásmundur",
        "Bjarki", "Bjartur", "Björn", "Brynjar", "Egill", "Erlendur", "Erlingur", "Eyvindur",
        "Hákon", "Hálfdan", "Hallbjörn", "Helgi", "Hjálmar", "Indriði", "Ingi", "Ingvar",
        "Sigmundur", "Sturla", "Sveinn", "Sverrir", "Þórarinn", "Þorbjörn", "Þorgrímur", "Þórður",
        "Birkir", "Breki", "Fannar"
    )

    private val SOBRENOMES_IRLANDESA = listOf(
        "Ó Briain", "Ó Conchobhair", "Ó Dálaigh", "Ó Domhnaill", "Ó Dubhghaill", "Ó Faoláin", "Ó Flannagáin", "Ó hAodha",
        "Ó hEidhin", "Ó Laoghaire", "Ó Maoilchiaráin", "Ó Murchadha", "Ó Néill", "Ó Riain", "Ó Ruairc", "Ó Súilleabháin",
        "Mac Aodha", "Mac Cárthaigh", "Mac Conmara", "Mac Diarmada", "Mac Donnchadha", "Mac Fhlannchadha", "Mac Giolla Phádraig", "Mac Lochlainn",
        "Mac Mathúna", "Mac Suibhne", "Ó Braonáin", "Ó Catháin", "Ó Cearbhaill", "Ó Ceallaigh", "Ó Cinnéide", "Ó Coileáin",
        "Ó Corráin", "Ó Cróinín", "Ó Duinn", "Ó Fearghail", "Ó Floinn", "Ó Gallchobhair", "Ó Gráda", "Ó hAllmhuráin",
        "Ó hAnluain", "Ó hEaghra", "Ó hIfearnáin", "Ó Maoláin", "Ó Mealláin", "Ó Mordha", "Ó Raghallaigh", "Ó Síocháin",
        "Ó Treasaigh", "Mac an Bhaird", "Mac Caba", "Mac Cionaoith", "Mac Eochagáin", "Mac Fhearghusa", "Mac Gabhann", "Mac Gearailt",
        "Mac Gréagóir", "Mac Íomhair", "Mac Raghnaill", "Mac Ruaidhrí"
    )

    private val SOBRENOMES_HUNGARA = listOf(
        "Farkas", "Nagy", "Kovács", "Szabó", "Tóth", "Varga", "Kiss", "Molnár",
        "Németh", "Balogh", "Lakatos", "Papp", "Takács", "Juhász", "Mészáros", "Oláh",
        "Simon", "Rácz", "Fekete", "Szilágyi", "Török", "Fehér", "Balázs", "Gál",
        "Kis", "Szőke", "Vörös", "Sipos", "Bíró", "Király", "Lukács", "Katona",
        "Sándor", "Vadász", "Hegedűs", "Bodnár", "Bakos", "Veres", "Barta", "Somogyi",
        "Kocsis", "Orbán", "Hajdu", "Antal", "Fodor", "Bálint", "Pintér", "Szalai",
        "Magyar", "Vass", "Almási", "Bakó", "Bán", "Bárdos", "Benkő", "Bodó",
        "Boros", "Borsos", "Budai", "Császár", "Csorba", "Dobozi", "Erdélyi", "Fazekas",
        "Fülöp", "Gáspár", "Gulyás", "Hajnal", "Halász", "Holló", "Kardos", "Kerekes",
        "Kertész", "Kormos", "Lantos", "Lázár", "Major", "Máté", "Miklós", "Nádas",
        "Nyíri", "Orosz", "Pálfi", "Pataki", "Rózsa", "Sárközi", "Sas", "Szekeres",
        "Székely", "Szűcs", "Tamási", "Vajda", "Váradi", "Végh", "Virág", "Zsoldos"
    )

    // Mapa único (Cultura, Gênero) -> banco de nomes, construído uma vez.
    // Substitui um "when" repetitivo de 8 branches quase idênticos — cada
    // cultura nova agora é só uma entrada aqui, em vez de precisar tocar
    // em dois lugares (bancoPara antigo tinha essa mesma lógica duplicada
    // pra cada uma das 8 culturas).
    private val bancos: Map<Pair<CulturaNome, GeneroNome>, List<String>> = mapOf(
        (CulturaNome.JAPONESA to GeneroNome.MASCULINO) to JAPONES_M,
        (CulturaNome.JAPONESA to GeneroNome.FEMININO) to JAPONES_F,
        (CulturaNome.CHINESA to GeneroNome.MASCULINO) to CHINES_M,
        (CulturaNome.CHINESA to GeneroNome.FEMININO) to CHINES_F,
        (CulturaNome.COREANA to GeneroNome.MASCULINO) to COREANO_M,
        (CulturaNome.COREANA to GeneroNome.FEMININO) to COREANO_F,
        (CulturaNome.ISLANDESA to GeneroNome.MASCULINO) to ISLANDES_M,
        (CulturaNome.ISLANDESA to GeneroNome.FEMININO) to ISLANDES_F,
        (CulturaNome.IRLANDESA_GAELICA to GeneroNome.MASCULINO) to IRLANDES_M,
        (CulturaNome.IRLANDESA_GAELICA to GeneroNome.FEMININO) to IRLANDES_F,
        (CulturaNome.HUNGARA to GeneroNome.MASCULINO) to HUNGARO_M,
        (CulturaNome.HUNGARA to GeneroNome.FEMININO) to HUNGARO_F,
        (CulturaNome.AFRICANA to GeneroNome.MASCULINO) to AFRICANO_M,
        (CulturaNome.AFRICANA to GeneroNome.FEMININO) to AFRICANO_F,
        (CulturaNome.ARABE to GeneroNome.MASCULINO) to ARABE_M,
        (CulturaNome.ARABE to GeneroNome.FEMININO) to ARABE_F,
        (CulturaNome.MESOAMERICANA to GeneroNome.MASCULINO) to MESOAMERICANO_M,
        (CulturaNome.MESOAMERICANA to GeneroNome.FEMININO) to MESOAMERICANO_F
    )

    private val sobrenomes: Map<CulturaNome, List<String>> = mapOf(
        CulturaNome.JAPONESA to SOBRENOMES_JAPONESA,
        CulturaNome.CHINESA to SOBRENOMES_CHINESA,
        CulturaNome.COREANA to SOBRENOMES_COREANA,
        CulturaNome.IRLANDESA_GAELICA to SOBRENOMES_IRLANDESA,
        CulturaNome.HUNGARA to SOBRENOMES_HUNGARA,
        CulturaNome.AFRICANA to SOBRENOMES_AFRICANA,
        CulturaNome.ARABE to SOBRENOMES_ARABE,
        CulturaNome.MESOAMERICANA to SOBRENOMES_MESOAMERICANA
    )

    // Solares e Lunares usam, por padrao, bancos sem sonoridade leste-asiatica.
    // Uma cultura escolhida explicitamente pelo usuario continua respeitada.
    private val CULTURAS_PADRAO_SOLAR_LUNAR = listOf(
        CulturaNome.ISLANDESA, CulturaNome.IRLANDESA_GAELICA,
        CulturaNome.HUNGARA, CulturaNome.AFRICANA,
        CulturaNome.ARABE, CulturaNome.MESOAMERICANA
    )

    fun gerarSolarOuLunar(
        cultura: CulturaNome? = null,
        genero: GeneroNome? = null,
        random: Random = Random.Default
    ): String {
        val escolhida = cultura ?: CULTURAS_PADRAO_SOLAR_LUNAR.random(random)
        val escolhido = genero ?: GeneroNome.entries.random(random)
        val quantidade = if (random.nextInt(100) < 60) 1 else random.nextInt(2, 4)
        return sortearComponentes(escolhida, escolhido, quantidade, random).joinToString(" ")
    }

    // Nomes e sobrenomes pertencem ao mesmo conjunto para compor NPCs.
    // Uma entrada composta (ex.: "Mac Aodha") permanece indivisível.
    internal fun sortearComponentes(
        cultura: CulturaNome,
        genero: GeneroNome,
        quantidade: Int,
        random: Random,
        excluidos: Collection<String> = emptyList()
    ): List<String> {
        val pool = (bancoPara(cultura, genero) + sobrenomes[cultura].orEmpty() +
            if (cultura == CulturaNome.ISLANDESA) SOBRENOMES_ISLANDESA_BASE else emptyList())
            .map { if (cultura == CulturaNome.CHINESA) normalizarReduplicacaoChinesa(it) else it }
            .distinctBy { it.trim().lowercase() }
            .filterNot { candidato -> excluidos.any { it.equals(candidato, ignoreCase = true) } }
        return pool.shuffled(random).take(quantidade)
    }

    // Para familias ja fornecidas (Casas do Imperio e Lookshy),
    // o prenome e sorteado diretamente, sem adicionar outro sobrenome.
    internal fun gerarPrenome(
        cultura: CulturaNome,
        genero: GeneroNome? = null,
        random: Random = Random.Default
    ): String {
        val generoEscolhido = genero ?: GeneroNome.entries.random(random)
        val sorteado = bancoPara(cultura, generoEscolhido).random(random)
        return if (cultura == CulturaNome.CHINESA) normalizarReduplicacaoChinesa(sorteado) else sorteado
    }

    private fun bancoPara(cultura: CulturaNome, genero: GeneroNome): List<String> =
        bancos.getValue(cultura to genero)

    // cultura/genero nulos = sorteia também a cultura/gênero, não só o nome
    // dentro de uma lista fixa — dá variedade real quando o usuário não
    // quer escolher manualmente.
    fun gerar(cultura: CulturaNome? = null, genero: GeneroNome? = null, random: Random = Random.Default): String {
        val culturaEscolhida = cultura ?: CulturaNome.entries.random(random)
        val generoEscolhido = genero ?: GeneroNome.entries.random(random)
        val nomeSorteado = bancoPara(culturaEscolhida, generoEscolhido).random(random)
        val nome = if (culturaEscolhida == CulturaNome.CHINESA) {
            normalizarReduplicacaoChinesa(nomeSorteado)
        } else {
            nomeSorteado
        }

        if (culturaEscolhida == CulturaNome.ISLANDESA) {
            val progenitor = SOBRENOMES_ISLANDESA_BASE.random(random)
            val radical = patronimicoIslandesRadical(progenitor)
            val sufixo = if (generoEscolhido == GeneroNome.MASCULINO) "son" else "dóttir"
            return "$nome $radical$sufixo"
        }

        val sobrenome = sobrenomes.getValue(culturaEscolhida).random(random)
        // Prenome e sobrenome são sorteados de bancos independentes. Algumas
        // culturas possuem termos válidos nos dois bancos (ex.: Yuan), mas
        // repetir exatamente o mesmo termo não forma um nome composto útil.
        // Normalizamos apenas esse caso, preservando nomes compostos distintos.
        if (sobrenome.equals(nome, ignoreCase = true)) return nome
        return when (culturaEscolhida) {
            CulturaNome.JAPONESA, CulturaNome.CHINESA, CulturaNome.COREANA, CulturaNome.HUNGARA -> "$sobrenome $nome"
            else -> "$nome $sobrenome"
        }
    }

    /**
     * Alguns prenomes chineses do banco usam reduplicação integral romanizada
     * (ex.: Yingying, Yuanyuan). Na Aba 11, a regra editorial é apresentar
     * apenas uma ocorrência nesses casos: Ying, Yuan.
     *
     * A normalização exige duas metades idênticas, portanto não altera compostos
     * legítimos como Qingyuan, Guiying ou Mingyuan.
     */
    internal fun normalizarReduplicacaoChinesa(nome: String): String {
        // O banco tambem pode receber formas separadas por espaco, como
        // "Ying Ying". Preservar nomes compostos com palavras distintas.
        val partes = nome.trim().split(Regex("\\s+"))
        if (partes.size == 2 && partes[0].equals(partes[1], ignoreCase = true)) {
            return partes[0]
        }
        if (nome.length < 4 || nome.length % 2 != 0) return nome
        val metade = nome.length / 2
        val primeira = nome.substring(0, metade)
        val segunda = nome.substring(metade)
        return if (primeira.equals(segunda, ignoreCase = true)) primeira else nome
    }

    /**
     * Genitivo simplificado para o conjunto controlado de nomes-base usado pelo gerador.
     * O objetivo é produzir patronímicos plausíveis sem fingir que sobrenomes islandeses
     * são famílias hereditárias.
     */
    private fun patronimicoIslandesRadical(nome: String): String = when (nome) {
        "Ari" -> "Ara"
        "Bjarni" -> "Bjarna"
        "Einar" -> "Einars"
        "Eiríkur" -> "Eiríks"
        "Gunnar" -> "Gunnars"
        "Hrafn" -> "Hrafns"
        "Leifur" -> "Leifs"
        "Ragnar" -> "Ragnars"
        "Snorri" -> "Snorra"
        "Úlfur" -> "Úlfs"
        "Þórir" -> "Þóris"
        "Þorsteinn" -> "Þorsteins"
        else -> nome.removeSuffix("ur").removeSuffix("r") + "s"
    }

    fun rotuloCultura(cultura: CulturaNome): String = when (cultura) {
        CulturaNome.JAPONESA -> "Japonesa"
        CulturaNome.CHINESA -> "Chinesa"
        CulturaNome.COREANA -> "Coreana"
        CulturaNome.ISLANDESA -> "Islandesa"
        CulturaNome.IRLANDESA_GAELICA -> "Irlandesa (Gaélica)"
        CulturaNome.HUNGARA -> "Húngara"
        CulturaNome.AFRICANA -> "Africana"
        CulturaNome.ARABE -> "Árabe"
        CulturaNome.MESOAMERICANA -> "Mesoamericana"
    }
}

// Origem de nome pra NPCs de Sangue de Dragão (Aba 11) — Solar continua
// usando NameGenerator.gerar() direto, sem passar por nada disso.
enum class OrigemNomeSangueDeDragao {
    IMPERIO, LOOKSHY, SEM_CASTA
}

object NomesSangueDeDragao {
    // Nomes de Casa do Ciclo Escarlate — apenas as mais conhecidas
    // ("providas" no pedido original), sem hierarquia entre elas.
    private val CASAS_IMPERIO = listOf(
        "Mnemon", "Peleps", "Tepet", "V'neef", "Cathak", "Sesus",
        "Ledaal", "Ragara", "Nellens", "Cynis", "Iselsi"
    )
    private val CASAS_LOOKSHY = listOf("Amilar", "Maheka", "Karal", "Teresu", "Yushoto")
    private val CULTURAS_IMPERIO = listOf(CulturaNome.CHINESA, CulturaNome.COREANA)
    private val CULTURAS_LOOKSHY = listOf(CulturaNome.JAPONESA, CulturaNome.COREANA)

    private fun gerarNomeComCasa(
        casa: String,
        culturas: List<CulturaNome>,
        genero: GeneroNome?,
        random: Random
    ): String {
        val cultura = culturas.random(random)
        // Sorteia o genero apenas uma vez: ambos os prenomes devem usar
        // o mesmo banco quando o usuario nao especificou genero.
        val generoEscolhido = genero ?: GeneroNome.entries.random(random)
        val quantidade = if (random.nextInt(100) < 70) 1 else random.nextInt(2, 4)
        val componentes = NameGenerator.sortearComponentes(
            cultura, generoEscolhido, quantidade, random, listOf(casa)
        )
        return (listOf(casa) + componentes).joinToString(" ")
    }

    fun gerar(origem: OrigemNomeSangueDeDragao, genero: GeneroNome? = null, random: Random = Random.Default): String {
        return when (origem) {
            OrigemNomeSangueDeDragao.SEM_CASTA -> {
                // Sem Casa dinástica: ainda há um nome de família, extraído
                // do mesmo conjunto cultural de nomes e sobrenomes.
                val cultura = CulturaNome.entries.random(random)
                val escolhido = genero ?: GeneroNome.entries.random(random)
                val familia = NameGenerator.sortearComponentes(cultura, escolhido, 1, random).first()
                val quantidade = if (random.nextInt(100) < 70) 1 else random.nextInt(2, 4)
                val adicionais = NameGenerator.sortearComponentes(
                    cultura, escolhido, quantidade, random, listOf(familia)
                )
                (listOf(familia) + adicionais).joinToString(" ")
            }
            OrigemNomeSangueDeDragao.IMPERIO -> {
                gerarNomeComCasa(CASAS_IMPERIO.random(random), CULTURAS_IMPERIO, genero, random)
            }
            OrigemNomeSangueDeDragao.LOOKSHY -> {
                gerarNomeComCasa(CASAS_LOOKSHY.random(random), CULTURAS_LOOKSHY, genero, random)
            }
        }
    }

    fun rotuloOrigem(origem: OrigemNomeSangueDeDragao): String = when (origem) {
        OrigemNomeSangueDeDragao.IMPERIO -> "Império"
        OrigemNomeSangueDeDragao.LOOKSHY -> "Lookshy"
        OrigemNomeSangueDeDragao.SEM_CASTA -> BoxNames.LunarCaste.CASTELESS
    }
}
