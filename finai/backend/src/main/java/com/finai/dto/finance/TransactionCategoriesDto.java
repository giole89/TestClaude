package com.finai.dto.finance;

import java.util.List;

/** Categorie note per popolare il menu a tendina di correzione manuale dei movimenti. */
public record TransactionCategoriesDto(List<String> income, List<String> expense) {}
