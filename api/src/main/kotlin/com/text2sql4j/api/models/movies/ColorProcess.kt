package com.text2sql4j.api.models.movies

/**
 * Taken from the dataset:
 * Process code {bws, bnw, col, \colprocess, cld}, if prc not set.
 *
 * <TR><TD>prc<TD>unknown<TD><TD>|
 * <TR><TD>col<TD>color<TD>color film, common after 1955<TD>|
 * <TR><TD>bnw<TD>black-and-white<TD>b-w film common before 1945<TD>|
 * <TR><TD>sbw<TD>silent<TD>silent black-and-white film <TD>|
 * <TR><TD>cld<TD>colored<TD>black-and-white film recolored<TD>|
 * <TR><TD>Cart<TD>cartoon<TD>Cartoons are normally colored<TD>|
 * <TR><TD>Tcol<TD>Technicolor<TD>high quality color  <TD>|
 * <TR><TD>Ecol<TD>Eastmancolor<TD> color by Kodak<TD> N\t(unstable)<td>|
 * <TR><TD>Wcol<TD>Warnercolor<TD><TD>|
 * <TR><TD>Mcol<TD>Metrocolor<TD> Color by MGM<TD>|
 * <TR><TD>Acol<TD>Anscocolor<TD>color by Kodak?<TD> |
 * <TR><TD>Agcol<TD>Agfacolor<TD><TD>|
 * <TR><TD>Fcol<TD>Fujicolor<TD><TD>|
 * <TR><TD>DeLuxe<TD>DeLuxe<TD>low cost color<TD>|
 * <TR><TD>DuArt<TD>DuArt<TD>color<TD>|
 * <TR><TD>Movielab<TD>MovieLab<TD>color<TD>|
 * <TR><TD>CS<TD>Cinemascope<TD>widescreen, mostly color <TD>|
 * <TR><TD>Trama <TD>Technirama<TD> widescreen color <TD>|
 * <TR><TD>Pan<TD>PanaVision<TD><TD>|
 * <TR><TD>TV<TD>film made for TV<TD>various processes<TD>|
 * <TR><TD>Vst<TD>Vistavision<TD><TD>|
 */
enum class ColorProcess(val code: String) {
    UNKNOWN("prc"),
    COLOR("col"),
    BLACK_AND_WHITE("bnw"),
    SILENT_BLACK_AND_WHITE("sbw"),
    BLACK_AND_WHITE_RECOLORED("cld"),
    CARTOON("Cart"),
    TECHNICOLOR("Tcol"),
    EASTMANCOLOR("Ecol"),
    WARNERCOLOR("Wcol"),
    METROCOLOR("Mcol"),
    ANSCOCOLOR("Acol"),
    AGFACOLOR("Agcol"),
    FUJICOLOR("Fcol"),
    DELUXE("DeLuxe"),
    DUART("DuArt"),
    MOVIELAB("Movielab"),
    CINEMASCOPE("CS"),
    TECHNIRAMA("Trama"),
    PANAVISION("Pan"),
    TV("TV"),
    VISTAVISION("Vst");

    companion object {
        fun fromCode(code: String): ColorProcess {
            return values().find { process -> process.code == code}
                ?: UNKNOWN
        }
    }
}