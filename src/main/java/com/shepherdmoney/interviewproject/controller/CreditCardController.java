package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.CreditCardBalance;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private UserRepository userRepository; 

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length
        CreditCard c = new CreditCard();
        c.setId(payload.getUserId());
        c.setIssuanceBank(payload.getCardIssuanceBank());
        c.setNumber(payload.getCardNumber());
        CreditCard savedCard = creditCardRepository.save(c);
        
        return ResponseEntity.ok(savedCard.getId());
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null
        
        User u = userRepository.getReferenceById(userId);
        List<CreditCardView> l = new ArrayList<>();
        for (CreditCard c : u.getCards()) {
            CreditCardView v = new CreditCardView(c.getIssuanceBank(), c.getNumber());
            l.add(v);
        }
        return ResponseEntity.ok(l); 

    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request
        try{
            creditCardRepository.getReferenceById(Integer.parseInt(creditCardNumber));
        }
        catch (jakarta.persistence.EntityNotFoundException j){
            return ResponseEntity.badRequest().body(Integer.parseInt(creditCardNumber));
        }
        
        return ResponseEntity.ok(Integer.parseInt(creditCardNumber));

    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<String> postMethodName(@RequestBody UpdateBalancePayload[] payload) {
        //TODO: Given a list of transactions, update credit cards' balance history.
        //      1. For the balance history in the credit card
        //      2. If there are gaps between two balance dates, fill the empty date with the balance of the previous date
        //      3. Given the payload `payload`, calculate the balance different between the payload and the actual balance stored in the database
        //      4. If the different is not 0, update all the following budget with the difference
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
        //      Given a balance amount of {date: 4/11, amount: 110}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 100}]
        //      This is because
        //      1. You would first populate 4/11 with previous day's balance (4/10), so {date: 4/11, amount: 100}
        //      2. And then you observe there is a +10 difference
        //      3. You propagate that +10 difference until today
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //        is not associated with a card.
        
        for (int i = 0; i < payload.length; i++) {
            UpdateBalancePayload transaction = payload[i];
            try { 
                CreditCard c = creditCardRepository.getReferenceById(Integer.parseInt(transaction.getCreditCardNumber()));
                List<CreditCardBalance> balance = c.getBalanceHistory();
                //sort by date
                balance.sort(Comparator.comparing(CreditCardBalance::getDate));
                int insert_ind = -1;
                double difference = 0;
                for (int j = 0; j < balance.size(); j++) {
                    //find first date that is after given date
                    if (balance.get(i).getDate().isAfter(transaction.getBalanceDate())) {
                        insert_ind = j;
                        difference = transaction.getBalanceAmount() - balance.get(j-1).getBalance();
                        CreditCardBalance newBalance = new CreditCardBalance();
                        newBalance.setCreditCard(c);
                        newBalance.setDate(transaction.getBalanceDate());
                        newBalance.setBalance(balance.get(j-1).getBalance());
                        balance.add(insert_ind, newBalance);
                        break;
                    }
                    //find if date exists
                    if (balance.get(j).getDate().equals(transaction.getBalanceDate())) {
                        difference = transaction.getBalanceAmount() - balance.get(j).getBalance();
                        balance.get(j).setBalance(transaction.getBalanceAmount());
                        insert_ind = j;
                        break;
                    }

                }
                //update accordingly if there exists a difference
                if (difference != 0) {
                    for (int j = insert_ind; j < balance.size(); j++) {
                        balance.get(j).setBalance(balance.get(j).getBalance() + difference);
                    }
                }



            } catch (jakarta.persistence.EntityNotFoundException j){
                return ResponseEntity.badRequest().body("Card Number Not Found");
            }

            
        }
        return ResponseEntity.ok("Success!");
    }
    
}
