from typing import List, Dict


def classify_criticality(pred_median: float, min_stock: int, optimal_stock: int) -> str:
    if pred_median <= min_stock:
        return 'CRITICAL'
    if pred_median <= optimal_stock * 0.5:
        return 'LOW'
    return 'OK'