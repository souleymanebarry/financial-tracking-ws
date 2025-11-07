package com.barry.bank.financial.tracking_ws.console;

import org.apache.commons.lang3.StringUtils;

public class Test {
    public static void main(String[] args) {

        String sourceAccountId = "LLBLJNLN";
        String destinationAccountId = "13080187";
        String description = String.format("Transfer from %s to %s", sourceAccountId, destinationAccountId);
        System.out.println(description);
        System.out.println("-------------------------------");

        String email = "";
        String email2 = null;
        //System.out.println(email.isBlank());
        System.out.println(StringUtils.isBlank(email));
        System.out.println(StringUtils.isBlank(null));

       // StringUtils.isBlank() :vérifie le blanc, le vide, la tabulation et le null

    }
}
