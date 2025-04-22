document.addEventListener('DOMContentLoaded', function() {
    let equipmentList = [];
    let equipmentCounter = 0;
    
    const addEquipmentBtn = document.getElementById('add-equipment');
    const calculateBtn = document.getElementById('calculate');
    const equipmentItems = document.getElementById('equipment-items');
    const resultsSection = document.getElementById('results');
    
    addEquipmentBtn.addEventListener('click', function() {
        const equipment = {
            id: equipmentCounter++,
            name: document.getElementById('name').value || `Обладнання ${equipmentCounter}`,
            efficiency: parseFloat(document.getElementById('efficiency').value),
            powerFactor: parseFloat(document.getElementById('power-factor').value),
            voltage: parseFloat(document.getElementById('voltage').value),
            quantity: parseInt(document.getElementById('quantity').value),
            nominalPower: parseFloat(document.getElementById('nominal-power').value),
            usageCoefficient: parseFloat(document.getElementById('usage-coefficient').value),
            reactivePowerCoefficient: parseFloat(document.getElementById('reactive-power-coefficient').value)
        };
        
        equipmentList.push(equipment);
        renderEquipmentList();
        clearInputs();
    });
    
    calculateBtn.addEventListener('click', function() {
        if (equipmentList.length === 0) {
            alert('Додайте хоча б одне обладнання!');
            return;
        }
        
        calculate();
        resultsSection.style.display = 'block';
    });
    
    function deleteEquipment(id) {
        equipmentList = equipmentList.filter(item => item.id !== id);
        renderEquipmentList();
    }
    
    function renderEquipmentList() {
        equipmentItems.innerHTML = '';
        
        equipmentList.forEach(item => {
            const row = document.createElement('tr');
            
            row.innerHTML = `
                <td>${item.name}</td>
                <td>${item.efficiency.toFixed(2)}</td>
                <td>${item.powerFactor.toFixed(2)}</td>
                <td>${item.voltage.toFixed(2)}</td>
                <td>${item.quantity}</td>
                <td>${item.nominalPower.toFixed(2)}</td>
                <td>${item.usageCoefficient.toFixed(2)}</td>
                <td>${item.reactivePowerCoefficient.toFixed(2)}</td>
                <td><button class="delete-btn" data-id="${item.id}">Видалити</button></td>
            `;
            
            equipmentItems.appendChild(row);
        });
        
        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                deleteEquipment(parseInt(this.getAttribute('data-id')));
            });
        });
    }

    function clearInputs() {
        document.getElementById('name').value = '';
        document.getElementById('efficiency').value = '0.92';
        document.getElementById('power-factor').value = '0.9';
        document.getElementById('voltage').value = '0.38';
        document.getElementById('quantity').value = '1';
        document.getElementById('nominal-power').value = '20';
        document.getElementById('usage-coefficient').value = '0.15';
        document.getElementById('reactive-power-coefficient').value = '1.33';
    }
    
    function calculate() {
        const totalNominalPower = equipmentList.reduce((sum, item) => 
            sum + item.quantity * item.nominalPower, 0);
        
        const totalActivePower = equipmentList.reduce((sum, item) => 
            sum + item.quantity * item.nominalPower * item.usageCoefficient, 0);
        const groupUsageCoefficient = totalActivePower / totalNominalPower;

        const numerator  = Math.pow(totalNominalPower, 2);
        const denominator = equipmentList.reduce((sum, item) => 
            sum + item.quantity * Math.pow(item.nominalPower, 2), 0);;
        const roundedEffectiveCount = numerator  / denominator;
        
        const activePowerCoefficient = getActivePowerCoefficient(roundedEffectiveCount, groupUsageCoefficient);
        
        const activeLoad = activePowerCoefficient * totalActivePower;
        
        const totalReactivePowerCoefficient = 1.0;
        const reactiveLoad = totalReactivePowerCoefficient * totalNominalPower * groupUsageCoefficient * 
            (equipmentList.reduce((sum, item) => sum + item.reactivePowerCoefficient, 0) / equipmentList.length);

        const fullPower = Math.sqrt(Math.pow(activeLoad, 2) + Math.pow(reactiveLoad, 2));
        
        const equipment = equipmentList[0];
        const groupCurrent = activeLoad / equipment.voltage;
        
        document.getElementById('n-pn').textContent = totalNominalPower.toFixed(0);
        document.getElementById('group-usage-coefficient').textContent = groupUsageCoefficient.toFixed(4);
        document.getElementById('effective-count').textContent = roundedEffectiveCount.toFixed(0);
        document.getElementById('active-power-coefficient').textContent = activePowerCoefficient.toFixed(2);
        document.getElementById('active-load').textContent = activeLoad.toFixed(2);
        document.getElementById('reactive-load').textContent = reactiveLoad.toFixed(3);
        document.getElementById('full-power').textContent = fullPower.toFixed(4);
        document.getElementById('group-current').textContent = groupCurrent.toFixed(2);
    }

    function getActivePowerCoefficient(roundedEffectiveCount, groupUsageCoefficient) {
        const table63 = {
            1: {0.1: 8.00, 0.15: 5.33, 0.2: 4.00, 0.3: 2.67, 0.4: 2.00, 0.5: 1.60, 0.6: 1.33, 0.7: 1.14, 0.8: 1},
            2: {0.1: 6.22, 0.15: 4.33, 0.2: 3.39, 0.3: 2.45, 0.4: 1.98, 0.5: 1.60, 0.6: 1.33, 0.7: 1.14, 0.8: 1},
            3: {0.1: 4.06, 0.15: 2.89, 0.2: 2.31, 0.3: 1.74, 0.4: 1.45, 0.5: 1.34, 0.6: 1.22, 0.7: 1.14, 0.8: 1},
            4: {0.1: 3.24, 0.15: 2.35, 0.2: 1.91, 0.3: 1.47, 0.4: 1.25, 0.5: 1.21, 0.6: 1.12, 0.7: 1.06, 0.8: 1},
            5: {0.1: 2.84, 0.15: 2.09, 0.2: 1.72, 0.3: 1.35, 0.4: 1.16, 0.5: 1.16, 0.6: 1.08, 0.7: 1.03, 0.8: 1},
            6: {0.1: 2.64, 0.15: 1.96, 0.2: 1.62, 0.3: 1.28, 0.4: 1.14, 0.5: 1.13, 0.6: 1.06, 0.7: 1.01, 0.8: 1},
            7: {0.1: 2.49, 0.15: 1.86, 0.2: 1.54, 0.3: 1.23, 0.4: 1.12, 0.5: 1.10, 0.6: 1.04, 0.7: 1.0, 0.8: 1},
            8: {0.1: 2.37, 0.15: 1.78, 0.2: 1.48, 0.3: 1.19, 0.4: 1.10, 0.5: 1.08, 0.6: 1.02, 0.7: 1.0, 0.8: 1},
            9: {0.1: 2.27, 0.15: 1.71, 0.2: 1.43, 0.3: 1.16, 0.4: 1.09, 0.5: 1.07, 0.6: 1.01, 0.7: 1.0, 0.8: 1},
            10: {0.1: 2.18, 0.15: 1.65, 0.2: 1.39, 0.3: 1.13, 0.4: 1.07, 0.5: 1.05, 0.6: 1.0, 0.7: 1.0, 0.8: 1},
            12: {0.1: 2.04, 0.15: 1.56, 0.2: 1.32, 0.3: 1.08, 0.4: 1.05, 0.5: 1.03, 0.6: 1.0, 0.7: 1.0, 0.8: 1},
            14: {0.1: 1.94, 0.15: 1.49, 0.2: 1.27, 0.3: 1.05, 0.4: 1.02, 0.5: 1.0, 0.6: 1.0, 0.7: 1.0, 0.8: 1},
            16: {0.1: 1.85, 0.15: 1.43, 0.2: 1.23, 0.3: 1.02, 0.4: 1.0, 0.5: 1.0, 0.6: 1.0, 0.7: 1.0, 0.8: 1},
            18: {0.1: 1.78, 0.15: 1.39, 0.2: 1.19, 0.3: 1.0, 0.4: 1.0, 0.5: 1.0, 0.6: 1.0, 0.7: 1.0, 0.8: 1},
            20: {0.1: 1.72, 0.15: 1.35, 0.2: 1.16, 0.3: 1.0, 0.4: 1.0, 0.5: 1.0, 0.6: 1.0, 0.7: 1.0, 0.8: 1},
            25: {0.1: 1.60, 0.15: 1.27, 0.2: 1.10, 0.3: 1.0, 0.4: 1.0, 0.5: 1.0, 0.6: 1.0, 0.7: 1.0, 0.8: 1},
            30: {0.1: 1.51, 0.15: 1.21, 0.2: 1.05, 0.3: 1.0, 0.4: 1.0, 0.5: 1.0, 0.6: 1.0, 0.7: 1.0, 0.8: 1},
            35: {0.1: 1.44, 0.15: 1.16, 0.2: 1.0, 0.3: 1.0, 0.4: 1.0, 0.5: 1.0, 0.6: 1.0, 0.7: 1.0, 0.8: 1},
            40: {0.1: 1.40, 0.15: 1.13, 0.2: 1.0, 0.3: 1.0, 0.4: 1.0, 0.5: 1.0, 0.6: 1.0, 0.7: 1.0, 0.8: 1},
            50: {0.1: 1.30, 0.15: 1.07, 0.2: 1.0, 0.3: 1.0, 0.4: 1.0, 0.5: 1.0, 0.6: 1.0, 0.7: 1.0, 0.8: 1},
            60: {0.1: 1.25, 0.15: 1.03, 0.2: 1.0, 0.3: 1.0, 0.4: 1.0, 0.5: 1.0, 0.6: 1.0, 0.7: 1.0, 0.8: 1},
            80: {0.1: 1.16, 0.15: 1.0, 0.2: 1.0, 0.3: 1.0, 0.4: 1.0, 0.5: 1.0, 0.6: 1.0, 0.7: 1.0, 0.8: 1},
            100: {0.1: 1.0, 0.15: 1.0, 0.2: 1.0, 0.3: 1.0, 0.4: 1.0, 0.5: 1.0, 0.6: 1.0, 0.7: 1.0, 0.8: 1}
        };
    
        const neValues = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 25, 30, 35, 40, 50, 60, 80, 100];
        
        const kvValues = [0.1, 0.15, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8];
        
        let closestNe = neValues[0];
        for (const ne of neValues) {
            if (ne <= roundedEffectiveCount) {
                closestNe = ne;
            } else {
                break;
            }
        }

        let closestKv = kvValues[0];
        for (const kv of kvValues) {
            if (kv <= groupUsageCoefficient) {
                closestKv = kv;
            } else {
                break;
            }
        }

        return table63[closestNe][closestKv];
    }
    
    function initializeExampleData() {
        const exampleEquipment = [
            {
                id: equipmentCounter++,
                name: "Шліфувальний верстат (1-4)",
                efficiency: 0.92,
                powerFactor: 0.9,
                voltage: 0.38,
                quantity: 4,
                nominalPower: 20,
                usageCoefficient: 0.15,
                reactivePowerCoefficient: 1.33
            },
            {
                id: equipmentCounter++,
                name: "Свердлильний верстат (5-6)",
                efficiency: 0.92,
                powerFactor: 0.9,
                voltage: 0.38,
                quantity: 2,
                nominalPower: 14,
                usageCoefficient: 0.12,
                reactivePowerCoefficient: 1.0
            },
            {
                id: equipmentCounter++,
                name: "Фугувальний верстат (9-12)",
                efficiency: 0.92,
                powerFactor: 0.9,
                voltage: 0.38,
                quantity: 4,
                nominalPower: 42,
                usageCoefficient: 0.15,
                reactivePowerCoefficient: 1.33
            },
            {
                id: equipmentCounter++,
                name: "Циркулярна пила (13)",
                efficiency: 0.92,
                powerFactor: 0.9,
                voltage: 0.38,
                quantity: 1,
                nominalPower: 36,
                usageCoefficient: 0.3,
                reactivePowerCoefficient: 1.52
            },
            {
                id: equipmentCounter++,
                name: "Прес (16)",
                efficiency: 0.92,
                powerFactor: 0.9,
                voltage: 0.38,
                quantity: 1,
                nominalPower: 20,
                usageCoefficient: 0.5,
                reactivePowerCoefficient: 0.75
            },
            {
                id: equipmentCounter++,
                name: "Полірувальний верстат (24)",
                efficiency: 0.92,
                powerFactor: 0.9,
                voltage: 0.38,
                quantity: 1,
                nominalPower: 40,
                usageCoefficient: 0.2,
                reactivePowerCoefficient: 1
            },
            {
                id: equipmentCounter++,
                name: "Фрезерний верстат (26-27)",
                efficiency: 0.92,
                powerFactor: 0.9,
                voltage: 0.38,
                quantity: 2,
                nominalPower: 32,
                usageCoefficient: 0.2,
                reactivePowerCoefficient: 1
            },
            {
                id: equipmentCounter++,
                name: "Вентилятор (36)",
                efficiency: 0.92,
                powerFactor: 0.9,
                voltage: 0.38,
                quantity: 1,
                nominalPower: 20,
                usageCoefficient: 0.65,
                reactivePowerCoefficient: 0.75
            }
        ];
        
        equipmentList = exampleEquipment;
        renderEquipmentList();
    }

    initializeExampleData();
});