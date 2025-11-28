// ForexPulse Application JavaScript

document.addEventListener('DOMContentLoaded', function() {
    initThemeToggle();
    initExchangeRateChart();
    initForm();
});

// Theme Toggle
function initThemeToggle() {
    const themeToggle = document.getElementById('themeToggle');
    const body = document.body;
    
    // Check saved theme preference
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'dark') {
        body.classList.remove('light-mode');
        body.classList.add('dark-mode');
    }
    
    themeToggle.addEventListener('click', function() {
        body.classList.toggle('light-mode');
        body.classList.toggle('dark-mode');
        
        // Save preference
        const isDark = body.classList.contains('dark-mode');
        localStorage.setItem('theme', isDark ? 'dark' : 'light');
        
        // Update charts for new theme
        updateChartsTheme();
    });
}

// Exchange Rate Chart
let exchangeRateChart = null;
let scenarioChart = null;
let marginRateChart = null;

function initExchangeRateChart() {
    const ctx = document.getElementById('exchangeRateChart');
    if (!ctx) return;
    
    const isDark = document.body.classList.contains('dark-mode');
    const textColor = isDark ? '#b0b0b0' : '#4a4a4a';
    const gridColor = isDark ? '#3a3a3a' : '#e0e0e0';
    
    exchangeRateChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: exchangeRateData.labels,
            datasets: [{
                label: 'USD/KRW 환율',
                data: exchangeRateData.rates,
                borderColor: '#ffcd00',
                backgroundColor: 'rgba(255, 205, 0, 0.1)',
                borderWidth: 3,
                fill: true,
                tension: 0.4,
                pointRadius: 0,
                pointHoverRadius: 6,
                pointHoverBackgroundColor: '#ffcd00',
                pointHoverBorderColor: '#fff',
                pointHoverBorderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    backgroundColor: isDark ? '#2c2c2c' : '#ffffff',
                    titleColor: isDark ? '#f5f5f5' : '#1a1a1a',
                    bodyColor: isDark ? '#b0b0b0' : '#4a4a4a',
                    borderColor: gridColor,
                    borderWidth: 1,
                    padding: 12,
                    displayColors: false,
                    callbacks: {
                        label: function(context) {
                            return context.parsed.y.toLocaleString('ko-KR') + '원';
                        }
                    }
                }
            },
            scales: {
                x: {
                    grid: {
                        color: gridColor,
                        drawBorder: false
                    },
                    ticks: {
                        color: textColor,
                        maxTicksLimit: 7
                    }
                },
                y: {
                    grid: {
                        color: gridColor,
                        drawBorder: false
                    },
                    ticks: {
                        color: textColor,
                        callback: function(value) {
                            return value.toLocaleString('ko-KR');
                        }
                    }
                }
            },
            interaction: {
                intersect: false,
                mode: 'index'
            }
        }
    });
}

function updateChartsTheme() {
    const isDark = document.body.classList.contains('dark-mode');
    const textColor = isDark ? '#b0b0b0' : '#4a4a4a';
    const gridColor = isDark ? '#3a3a3a' : '#e0e0e0';
    
    const charts = [exchangeRateChart, scenarioChart, marginRateChart];
    
    charts.forEach(chart => {
        if (chart) {
            chart.options.scales.x.grid.color = gridColor;
            chart.options.scales.x.ticks.color = textColor;
            chart.options.scales.y.grid.color = gridColor;
            chart.options.scales.y.ticks.color = textColor;
            chart.options.plugins.tooltip.backgroundColor = isDark ? '#2c2c2c' : '#ffffff';
            chart.options.plugins.tooltip.titleColor = isDark ? '#f5f5f5' : '#1a1a1a';
            chart.options.plugins.tooltip.bodyColor = isDark ? '#b0b0b0' : '#4a4a4a';
            chart.update();
        }
    });
}

// Form Handling
function initForm() {
    const form = document.getElementById('companyInputForm');
    if (!form) return;
    
    form.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        const formData = {
            materialCostUsd: parseFloat(document.getElementById('materialCostUsd').value),
            materialRatio: parseFloat(document.getElementById('materialRatio').value),
            sellingPriceKrw: parseFloat(document.getElementById('sellingPriceKrw').value),
            targetMarginRate: parseFloat(document.getElementById('targetMarginRate').value),
            otherCostsKrw: parseFloat(document.getElementById('otherCostsKrw').value)
        };
        
        await performAnalysis(formData);
    });
}

async function performAnalysis(formData) {
    showLoading();
    
    try {
        const response = await fetch('/api/analyze', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });
        
        if (!response.ok) {
            throw new Error('분석 요청 실패');
        }
        
        const data = await response.json();
        updateAnalysisResults(data);
        
        // Show analysis section
        document.getElementById('analysisResults').classList.remove('hidden');
        
        // Scroll to results
        document.getElementById('analysisResults').scrollIntoView({ behavior: 'smooth' });
        
    } catch (error) {
        console.error('Analysis error:', error);
        alert('분석 중 오류가 발생했습니다. 다시 시도해주세요.');
    } finally {
        hideLoading();
    }
}

function updateAnalysisResults(data) {
    // Real-time Profit/Loss
    const pl = data.realTimeProfitLoss;
    document.getElementById('currentCost').textContent = formatCurrency(pl.currentCost);
    document.getElementById('costChange').textContent = formatPercentChange(pl.costChangeRate30Day) + ' (30일 전 대비)';
    document.getElementById('costChange').className = 'metric-change ' + (pl.costChangeRate30Day >= 0 ? 'positive' : 'negative');
    document.getElementById('currentMargin').textContent = formatCurrency(pl.currentMargin);
    document.getElementById('currentMarginRate').textContent = '마진율 ' + pl.currentMarginRate + '%';
    document.getElementById('targetMargin').textContent = formatCurrency(pl.targetMargin);
    document.getElementById('targetMarginRateDisplay').textContent = '목표 ' + pl.targetMarginRate + '%';
    document.getElementById('targetGap').textContent = formatCurrency(pl.targetGap);
    
    const targetStatus = document.getElementById('targetStatus');
    targetStatus.textContent = pl.targetAchieved ? '목표 달성' : '목표 미달';
    targetStatus.className = 'metric-badge ' + (pl.targetAchieved ? 'achieved' : 'not-achieved');
    
    // Order Timing Guide
    const timing = data.orderTimingGuide;
    document.getElementById('breakEvenRate').textContent = formatRate(timing.breakEvenExchangeRate);
    document.getElementById('breakEvenMessage').textContent = timing.breakEvenMessage;
    document.getElementById('targetExchangeRate').textContent = formatRate(timing.targetExchangeRate);
    document.getElementById('targetExchangeMessage').textContent = timing.targetMessage;
    
    // Exchange Rate Status
    const status = data.exchangeRateStatus;
    document.getElementById('statusMin').textContent = formatRate(status.minRange);
    document.getElementById('statusMax').textContent = formatRate(status.maxRange);
    
    const indicator = document.getElementById('statusIndicator');
    indicator.style.left = status.position + '%';
    indicator.textContent = Math.round(status.currentRate);
    
    const statusBadge = document.getElementById('statusBadge');
    statusBadge.textContent = status.statusMessage;
    statusBadge.className = 'status-badge ' + status.statusLevel;
    
    document.getElementById('statusMessage').textContent = '현재 환율: ' + formatRate(status.currentRate);
    document.getElementById('aiEvaluation').textContent = status.aiEvaluation;
    
    // Monitoring Strategy
    document.getElementById('monitoringStrategy').textContent = data.monitoringStrategy;
    
    // Scenario Analysis Chart
    updateScenarioChart(data.scenarioAnalysisList);
    
    // Margin Rate Change Chart
    updateMarginRateChart(data.marginRateChanges);
    
    // Detailed Cost Analysis
    const detail = data.detailedCostAnalysis;
    document.getElementById('detailMaterialUsd').textContent = '$' + formatNumber(detail.materialCostUsd);
    document.getElementById('detailExchangeRate').textContent = formatRate(detail.appliedExchangeRate) + ' (실시간)';
    document.getElementById('detailMaterialKrw').textContent = formatCurrency(detail.materialCostKrw);
    document.getElementById('detailOtherCosts').textContent = formatCurrency(detail.otherCosts);
    document.getElementById('detailTotalCost').textContent = formatCurrency(detail.totalCost);
    document.getElementById('detailSellingPrice').textContent = formatCurrency(detail.sellingPrice);
    document.getElementById('detailNetMargin').textContent = formatCurrency(detail.netMargin) + ' (' + detail.netMarginRate + '%)';
}

function updateScenarioChart(scenarios) {
    const ctx = document.getElementById('scenarioChart');
    if (!ctx) return;
    
    if (scenarioChart) {
        scenarioChart.destroy();
    }
    
    const isDark = document.body.classList.contains('dark-mode');
    const textColor = isDark ? '#b0b0b0' : '#4a4a4a';
    const gridColor = isDark ? '#3a3a3a' : '#e0e0e0';
    
    const labels = scenarios.map(s => s.exchangeRate.toLocaleString('ko-KR') + '원');
    const costData = scenarios.map(s => s.cost);
    const marginData = scenarios.map(s => s.margin);
    
    const backgroundColors = scenarios.map(s => s.isCurrent ? 'rgba(255, 205, 0, 0.8)' : 'rgba(255, 205, 0, 0.4)');
    const marginColors = scenarios.map(s => s.margin >= 0 ? 'rgba(34, 197, 94, 0.7)' : 'rgba(239, 68, 68, 0.7)');
    
    scenarioChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [
                {
                    label: '원가',
                    data: costData,
                    backgroundColor: 'rgba(239, 68, 68, 0.7)',
                    borderRadius: 4
                },
                {
                    label: '마진',
                    data: marginData,
                    backgroundColor: marginColors,
                    borderRadius: 4
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'top',
                    labels: {
                        color: textColor
                    }
                },
                tooltip: {
                    backgroundColor: isDark ? '#2c2c2c' : '#ffffff',
                    titleColor: isDark ? '#f5f5f5' : '#1a1a1a',
                    bodyColor: isDark ? '#b0b0b0' : '#4a4a4a',
                    borderColor: gridColor,
                    borderWidth: 1,
                    callbacks: {
                        label: function(context) {
                            return context.dataset.label + ': ' + formatCurrency(context.parsed.y);
                        }
                    }
                }
            },
            scales: {
                x: {
                    grid: {
                        color: gridColor,
                        drawBorder: false
                    },
                    ticks: {
                        color: textColor
                    }
                },
                y: {
                    grid: {
                        color: gridColor,
                        drawBorder: false
                    },
                    ticks: {
                        color: textColor,
                        callback: function(value) {
                            return (value / 10000).toLocaleString('ko-KR') + '만';
                        }
                    }
                }
            }
        }
    });
}

function updateMarginRateChart(changes) {
    const ctx = document.getElementById('marginRateChart');
    if (!ctx) return;
    
    if (marginRateChart) {
        marginRateChart.destroy();
    }
    
    const isDark = document.body.classList.contains('dark-mode');
    const textColor = isDark ? '#b0b0b0' : '#4a4a4a';
    const gridColor = isDark ? '#3a3a3a' : '#e0e0e0';
    
    const labels = changes.map(c => c.exchangeRate.toLocaleString('ko-KR'));
    const marginRates = changes.map(c => c.marginRate);
    
    marginRateChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: '마진율 (%)',
                data: marginRates,
                borderColor: '#22c55e',
                backgroundColor: 'rgba(34, 197, 94, 0.1)',
                borderWidth: 3,
                fill: true,
                tension: 0.4,
                pointRadius: 2,
                pointHoverRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    backgroundColor: isDark ? '#2c2c2c' : '#ffffff',
                    titleColor: isDark ? '#f5f5f5' : '#1a1a1a',
                    bodyColor: isDark ? '#b0b0b0' : '#4a4a4a',
                    borderColor: gridColor,
                    borderWidth: 1,
                    callbacks: {
                        title: function(context) {
                            return '환율: ' + context[0].label + '원';
                        },
                        label: function(context) {
                            return '마진율: ' + context.parsed.y.toFixed(2) + '%';
                        }
                    }
                }
            },
            scales: {
                x: {
                    title: {
                        display: true,
                        text: '환율 (원/USD)',
                        color: textColor
                    },
                    grid: {
                        color: gridColor,
                        drawBorder: false
                    },
                    ticks: {
                        color: textColor,
                        maxTicksLimit: 10
                    }
                },
                y: {
                    title: {
                        display: true,
                        text: '마진율 (%)',
                        color: textColor
                    },
                    grid: {
                        color: gridColor,
                        drawBorder: false
                    },
                    ticks: {
                        color: textColor,
                        callback: function(value) {
                            return value + '%';
                        }
                    }
                }
            }
        }
    });
}

// Utility Functions
function formatCurrency(value) {
    return new Intl.NumberFormat('ko-KR', {
        style: 'currency',
        currency: 'KRW',
        maximumFractionDigits: 0
    }).format(value);
}

function formatNumber(value) {
    return new Intl.NumberFormat('ko-KR', {
        maximumFractionDigits: 2
    }).format(value);
}

function formatRate(value) {
    return new Intl.NumberFormat('ko-KR', {
        style: 'decimal',
        minimumFractionDigits: 1,
        maximumFractionDigits: 1
    }).format(value) + '원/USD';
}

function formatPercentChange(value) {
    const sign = value >= 0 ? '+' : '';
    return sign + value.toFixed(2) + '%';
}

function showLoading() {
    document.getElementById('loadingOverlay').classList.remove('hidden');
}

function hideLoading() {
    document.getElementById('loadingOverlay').classList.add('hidden');
}
