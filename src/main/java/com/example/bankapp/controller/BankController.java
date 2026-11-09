package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.service.BankService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Map;

@Controller
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        populateDashboard(model, keyword);
        return "bank";
    }

    @PostMapping("/customers")
    public String addCustomer(@Valid @ModelAttribute("customer") Customer customer,
                              BindingResult bindingResult, Model model,
                              RedirectAttributes redirectAttributes,
                              @RequestParam(required = false) String keyword) {
        if (bindingResult.hasErrors()) {
            populateDashboard(model, keyword);
            return "bank";
        }
        try {
            bankService.addCustomer(customer);
            redirectAttributes.addFlashAttribute("success", "Customer added successfully");
        } catch (BankException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transferRequest") TransferRequest request,
                           BindingResult bindingResult, Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateDashboard(model, null);
            return "bank";
        }
        try {
            bankService.transfer(request.getFromAccount(), request.getToAccount(),
                    request.getAmount(), request.isSimulateFailure());
            redirectAttributes.addFlashAttribute("success", "Transfer completed successfully");
        } catch (BankException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping(value = "/transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> transferJson(@Valid @RequestBody TransferRequest request) {
        try {
            bankService.transfer(request.getFromAccount(), request.getToAccount(),
                    request.getAmount(), request.isSimulateFailure());
            return ResponseEntity.ok(Map.of("status", "success", "message", "Transfer completed successfully"));
        } catch (BankException exception) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", exception.getMessage()));
        }
    }

    @PostMapping(value = "/api/transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> transferApiJson(@Valid @RequestBody TransferRequest request) {
        return transferJson(request);
    }

    @GetMapping("/deposit")
    public String depositPage(Model model) {
        model.addAttribute("customers", bankService.getCustomers(null));
        return "deposit";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam String accountNumber, @RequestParam BigDecimal amount,
                          RedirectAttributes redirectAttributes) {
        try {
            bankService.deposit(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Deposit completed successfully");
        } catch (BankException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/deposit";
    }

    @PostMapping(value = "/deposit", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> depositJson(@RequestBody Map<String, Object> payload) {
        try {
            String accountNumber = String.valueOf(payload.get("accountNumber"));
            BigDecimal amount = new BigDecimal(String.valueOf(payload.get("amount")));
            bankService.deposit(accountNumber, amount);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Deposit completed successfully"));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", exception.getMessage()));
        }
    }

    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        model.addAttribute("customers", bankService.getCustomers(null));
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam String accountNumber, @RequestParam BigDecimal amount,
                           RedirectAttributes redirectAttributes) {
        try {
            bankService.withdraw(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Withdrawal completed successfully");
        } catch (BankException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/withdraw";
    }

    @PostMapping(value = "/withdraw", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> withdrawJson(@RequestBody Map<String, Object> payload) {
        try {
            String accountNumber = String.valueOf(payload.get("accountNumber"));
            BigDecimal amount = new BigDecimal(String.valueOf(payload.get("amount")));
            bankService.withdraw(accountNumber, amount);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Withdrawal completed successfully"));
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", exception.getMessage()));
        }
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(@PathVariable String accountNumber, Model model) {
        model.addAttribute("customer", bankService.getCustomer(accountNumber));
        model.addAttribute("transactions", bankService.getTransactionsForAccount(accountNumber));
        model.addAttribute("totalSent", bankService.getTotalSent(accountNumber));
        return "history";
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(@RequestParam(defaultValue = "10000") BigDecimal min, Model model) {
        model.addAttribute("min", min);
        model.addAttribute("transfers", bankService.getLargeTransfers(min));
        return "large-transfers";
    }

    private void populateDashboard(Model model, String keyword) {
        model.addAttribute("customers", bankService.getCustomers(keyword));
        model.addAttribute("allCustomers", bankService.getAllCustomers());
        model.addAttribute("transactions", bankService.getRecentTransactions());
        model.addAttribute("totalCustomers", bankService.countCustomers());
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("successCount", bankService.countTransactions(TransactionStatus.SUCCESS));
        model.addAttribute("failedCount", bankService.countTransactions(TransactionStatus.FAILED));
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }
        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
    }
}
