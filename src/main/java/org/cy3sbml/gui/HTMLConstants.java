package org.cy3sbml.gui;

public class HTMLConstants {
    public static final String HTML_START_TEMPLATE =
            "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "\t<base href=\"%s\" />\n" +
                    "\t<meta charset=\"utf-8\">\n" +
                    "\t<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
                    "\t<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                    "\t<title>%s</title>\n" +
                    "\t<link rel=\"shortcut icon\" href=\"./images/favicon.ico\" />\n" +
                    "\t<link rel=\"stylesheet\" href=\"./css/bootstrap.min.css\">\n" +
                    "\t<link rel=\"stylesheet\" href=\"./css/jquery.dataTables.min.css\">\n" +
                    "\t<link rel=\"stylesheet\" href=\"./font-awesome-4.6.3/css/font-awesome.min.css\">\n" +
                    "\t<link rel=\"stylesheet\" href=\"./css/cy3sbml.css\">\n" +
                    "</head>\n\n" +
                    "<body>\n" +
                    "<div class=\"container\">\n";

    public static final String HTML_STOP_TEMPLATE =
            "</div>\n" +
                    "<script type=\"text/javascript\" language=\"javascript\" src=\"./js/jquery-1.12.3.js\"></script>\n" +
                    "<script type=\"text/javascript\" language=\"javascript\" src=\"./js/bootstrap.min.js\"></script>\n" +
                    "<script type=\"text/javascript\" language=\"javascript\" src=\"./js/jquery.dataTables.min.js\"></script>\n" +
                    "\t<script type=\"text/javascript\" language=\"javascript\">\n" +
                    "\t$(document).ready(function() {\n" +
                    "\t    $('#table').DataTable();\n" +
                    "\t} );\n" +
                    "\t</script>\n" +
                    "</body>\n" +
                    "</html>\n";

    public static final String ICON_WARNING = "<span class=\"fa fa-exclamation-circle fa-lg\" title=\"true\" style=\"color:red\"> </span>";
    public static final String ICON_TRUE = "<span class=\"fa fa-check-circle fa-lg\" title=\"true\" style=\"color:green\"> </span>";
    public static final String ICON_FALSE = "<span class=\"fa fa-times-circle fa-lg\" title=\"false\" style=\"color:red\"> </span>";
    public static final String ICON_NONE = "<span class=\"fa fa-circle-o fa-lg\" title=\"none\" style=\"color:grey\"> </span>";
    public static final String ICON_INVISIBLE = "<span class=\"fa fa-circle-o fa-lg icon-invisible\" title=\"none\"> </span>";

}
