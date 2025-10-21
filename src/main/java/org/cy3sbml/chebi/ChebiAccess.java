package org.cy3sbml.chebi;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cy3sbml.gui.GUIConstants;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.cy3sbml.gui.GUIConstants.*;


/**
 * Access Chebi information.
 */
public class ChebiAccess {

    private static final Logger logger = LoggerFactory.getLogger(ChebiAccess.class);
    public static Map<String, String> htmlFragments = GUIConstants.htmlFragments;

    /**
     * Perform queries and get chebi information.
     * @param identifier: chebi identifier of form CHEBI:15377
     * @return HTML presentation of chebi information
     */
    public static String getChebiHTML(String identifier) {
        String[] tokens = identifier.split(":");
        String chebiNumber = tokens[1];
        String json = null;
        String svg = null;

        // retrieve chebi information
        String url = String.format("https://www.ebi.ac.uk/chebi/backend/api/public/compound/%s/", chebiNumber);
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            json = response.body();
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage(), e);
        }

        // retrieve chebi structure
        url = String.format("https://www.ebi.ac.uk/chebi/backend/api/public/compound/%s/structure/?width=300&height=300", chebiNumber);
        request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            svg = response.body();
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage(), e);
        }

        // HTML display
        //   "chemical_data": {
        //    "formula": "H2O",
        //    "charge": 0,
        //    "mass": "18.015",
        //    "monoisotopic_mass": "18.01056"
        //  },
        String text = "";
        String formula;
        String charge;
        String mass;

        JSONObject obj = JSON.parseObject(json);
        if (obj != null) {
            try {
                JSONObject chemical_data = obj.getJSONObject("chemical_data");
                if (chemical_data != null) {
                    formula = chemical_data.getString("formula");
                    charge = chemical_data.getString("charge");
                    mass = chemical_data.getString("mass");
                    text += String.format(
                            TABLE_START +
                                    TS + "Formula" + TM + "%s" + TE +
                                    TS + "Charge" + TM + "%s" + TE +
                                    TS + "Mass" + TM + "%s" + TE +
                                    TABLE_END,
                            formula, charge, mass
                    );
                }
            } catch (JSONException ignored) {}
        }
        // add image
        if (svg != null) {
            text += String.format(
                    "<a href=\"https://www.ebi.ac.uk/chebi/%s\">%s</a><br />\n",
                    identifier, svg);

        }
        return text;
    }
}
