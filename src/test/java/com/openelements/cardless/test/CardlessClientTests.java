package com.openelements.cardless.test;

import com.openelements.cardless.CardlessClient;
import com.openelements.cardless.Institution;
import com.openelements.cardless.Requisition;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CardlessClientTests {

    @Test
    void test() throws Exception {
        //given
        Dotenv dotenv = Dotenv.load();
        String secretId = dotenv.get("CARDLESS_SECRET_ID");
        String secretKey = dotenv.get("CARDLESS_SECRET_KEY");
        CardlessClient client = new CardlessClient(secretId, secretKey);

        //when
        List<Institution> institutions = client.getInstitutions("de");

        //then
        final Institution bank = institutions.stream()
                .filter(institution -> institution.name().equals("Sparkasse Dortmund"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Institution not found"));

        final Requisition requisition = client.createRequisition(bank);

        System.out.println("Please open: " + requisition.link());
    }
}
