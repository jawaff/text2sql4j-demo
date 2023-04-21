package com.text2sql4j.api.models.movies

/**
 * Taken from the dataset:
 * Category code {BioP, Disa (disaster), Dram, CnR, Comd, Faml, Hist, Horr, Musc, Noir, Romt, ScFi, Susp}, if Ctxx not set.
 *
 * <TR><TD>Ctxx<TD>uncategorized<TD>|
 * <TR><TD>Actn<TD>violence<TD>|
 * <TD>Advt<TD>adventure<TD>|
 * <TD>AvGa<TD>Avant Garde<TD>|
 * <TD>Cart<TD>cartoon<TD>|
 * <TD>CnR <TD>Cops and Robbers<TD>|
 * <TR><TD>Comd<TD>comedy<TD>|
 * <TR><TD>Disa<TD>Disaster<TD>|
 * <TD> Docu<TD>documentary<TD>
 * <TD>Dram<TD>drama<TD>|
 * <TR><TD>Epic<TD>epic<TD>|
 * <TD>Faml<TD>family<TD>     |
 * <TD>Hist<TD>history<TD>    |
 * <TR><TD>Horr<TD>horror<TD>|
 * <TD>Musc<TD>musical<TD>|
 * <TD>Myst<TD>mystery<TD>|
 * <TR><TD>Noir<TD>black<TD>|
 * <TD>Porn<TD>pornography<TD>|
 * <TD>Romt<TD>romantic<TD>|
 * <TR><TD>ScFi<TD>science fiction<TD>|
 * <TD>Surl<TD>sureal<TD>|
 * <TD>Susp<TD>thriller<TD>   |
 * <TR><TD>West<TD>western<TD> |
 * <TR><TD>  BioP <TD>biographical Picture<TD>|
 * <TR><TD> TV <TD>TV show<TD>|
 * <TR><TD> TVs <TD>TV series<TD>|
 * <TR><TD> TVm <TD>TV miniseries<TD>|
 */
enum class Genre(val code: String) {
    UNCATEGORIZED("Ctxx"),
    ACTION("Actn"),
    ADVENTURE("Advt"),
    AVANT_GARDE("AvGa"),
    CARTOON("Cart"),
    COPS_AND_ROBBERS("CnR"),
    COMEDY("Comd"),
    DISASTER("Disa"),
    DOCUMENTARY("Docu"),
    DRAMA("Dram"),
    EPIC("Epic"),
    FAMILY("Faml"),
    HISTORY("Hist"),
    HORROR("Horr"),
    MUSICAL("Musc"),
    MYSTERY("Myst"),
    NOIR("Noir"),
    PORNOGRAPHY("Porn"),
    ROMANTIC("Romt"),
    SCIENCE_FICTION("ScFi"),
    SUREAL("Surl"),
    THRILLER("Susp"),
    WESTERN("West"),
    BIOGRAPHICAL_PICTURE("BioP"),
    TV_SHOW("TV"),
    TV_SERIES("TVs"),
    TV_MINI_SERIES("TVm");

    companion object {
        fun fromCode(code: String): Genre {
            return values().find { category -> category.code == code }
                ?: UNCATEGORIZED
        }
    }
}