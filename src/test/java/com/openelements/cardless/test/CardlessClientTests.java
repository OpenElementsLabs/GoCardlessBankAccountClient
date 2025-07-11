package com.openelements.cardless.test;

import com.openelements.cardless.CardlessClient;
import com.openelements.cardless.data.Institution;
import com.openelements.cardless.data.Requisition;
import com.openelements.cardless.data.RequisitionsPage;
import io.github.cdimascio.dotenv.Dotenv;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CardlessClientTests {

    @Test
    void test1() throws Exception {
        //given
        Dotenv dotenv = Dotenv.load();
        String secretId = dotenv.get("CARDLESS_SECRET_ID");
        String secretKey = dotenv.get("CARDLESS_SECRET_KEY");
        CardlessClient client = CardlessClient.create(secretId, secretKey);

        //when
        List<Institution> institutions = client.getInstitutions("de");

        //then
        final Institution bank = institutions.stream()
                .filter(institution -> institution.name().equals("Sparkasse Dortmund"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Institution not found"));

        final Requisition requisition = client.createRequisition(bank.id(), URI.create("https://example.com/"));

        System.out.println("Please open: " + requisition.link());
    }

    @Test
    void test1_5() throws Exception {
        //given
        Dotenv dotenv = Dotenv.load();
        String secretId = dotenv.get("CARDLESS_SECRET_ID");
        String secretKey = dotenv.get("CARDLESS_SECRET_KEY");
        CardlessClient client = CardlessClient.create(secretId, secretKey);

        final Requisition requisition = client.createRequisition("SANDBOXFINANCE_SFIN0000",
                URI.create("https://example.com/"));

        System.out.println("Please open: " + requisition.link());
    }

    @Test
    void test2() throws Exception {
        //given
        Dotenv dotenv = Dotenv.load();
        String secretId = dotenv.get("CARDLESS_SECRET_ID");
        String secretKey = dotenv.get("CARDLESS_SECRET_KEY");
        CardlessClient client = CardlessClient.create(secretId, secretKey);

        //when
        RequisitionsPage page = client.getRequisitions(10, 0);
        page.requisitions().stream()
                .flatMap(r -> r.accounts().stream())
                .map(a -> {
                    try {
                        return client.getTransactions(a);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).flatMap(t -> t.bookedTransactions().stream())
                .forEach(t -> System.out.println(
                        t.bookingDate() + ": " + t.remittanceInformationUnstructured() + " -> " + t.transactionAmount()
                                .amount() + " " + t.transactionAmount().currency()));
    }

    @Test
    void test3() throws Exception {
        //given
        Dotenv dotenv = Dotenv.load();
        String secretId = dotenv.get("CARDLESS_SECRET_ID");
        String secretKey = dotenv.get("CARDLESS_SECRET_KEY");
        CardlessClient client = CardlessClient.create(secretId, secretKey);

        //when
        RequisitionsPage page = client.getRequisitions(10, 0);
        page.requisitions().stream()
                .flatMap(r -> r.accounts().stream())
                .flatMap(a -> {
                    try {
                        return client.getBalances(a).stream();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .forEach(b -> System.out.println(
                        b.balanceType() + ": " + b.balanceAmount() + " " + b.referenceDate()));
    }
}
