const HOURS_IN_YEAR = 8760;

const Elements = [
  { id: 'line1', name: 'Лінія електропередач 1', omega: 0.1, tB: 2 },
  { id: 'line2', name: 'Лінія електропередач 2', omega: 0.2, tB: 1 },
  { id: 'transformer1', name: 'Трансформатор 1', omega: 0.05, tB: 4 },
  { id: 'transformer2', name: 'Трансформатор 2', omega: 0.15, tB: 3 }
];

const SectionalSwitches = [
  { id: 'none', name: 'Відсутній', kSection: 1 },
  { id: 'with', name: 'Є', kSection: 0.5 }
];

let selectedElements = [];
let sectionalSwitch = 'none';

document.addEventListener('DOMContentLoaded', App);

const getEl = id => document.getElementById(id);
const getInputValue = id => parseFloat(getEl(id).value) || 0;
const setFormattedResult = (id, value) => {
  getEl(id).querySelector('span').textContent = formatNumber(value);
};
const updateResultField = (id, value, decimals = 2) => {
  getEl(id).textContent = value.toFixed(decimals);
};
const formatNumber = value => value.toLocaleString('uk-UA', { maximumFractionDigits: 2 });
const toggleVisibility = (el, show) => el.classList.toggle('hidden', !show);

function App() {
  populateDropdown(getEl('elementSelect'), Elements, 'id', 'name');
  populateDropdown(getEl('sectionalSwitchSelect'), SectionalSwitches, 'id', 'name');
  bindEvents();
  LossesCalculator.calculate();
}

function bindEvents() {
  getEl('addElementBtn').addEventListener('click', ElementManager.handleAdd);
  getEl('sectionalSwitchSelect').addEventListener('change', ElementManager.handleSwitchChange);
}

function populateDropdown(select, items, valueKey, labelKey) {
  select.innerHTML = '<option disabled selected value="">Оберіть</option>';
  items.forEach(item => {
    const option = new Option(item[labelKey], item[valueKey]);
    select.appendChild(option);
  });
}

function renderTable() {
  const container = getEl('elementsTableBody');
  const show = selectedElements.length > 0;
  container.innerHTML = '';
  toggleVisibility(getEl('elementsTableContainer'), show);
  toggleVisibility(getEl('resultsContainer'), show);
  selectedElements.forEach((el, i) => {
    const row = document.createElement('tr');
    row.append(
      createCell(el.name),
      createCell(el.omega, 'center'),
      createCell(el.tB, 'center'),
      createQuantityCell(i, el.quantity),
      createActionCell(i)
    );
    container.appendChild(row);
  });
}

function createCell(text, className = '') {
  const td = document.createElement('td');
  td.textContent = text;
  if (className) td.className = className;
  return td;
}

function createQuantityCell(index, value) {
  const td = createCell('');
  td.className = 'center';
  const input = Object.assign(document.createElement('input'), {
    type: 'number',
    min: '1',
    value,
    className: 'quantity-input',
    onchange: e => ElementManager.updateQuantity(index, +e.target.value || 1)
  });
  td.appendChild(input);
  return td;
}

function createActionCell(index) {
  const td = createCell('');
  td.className = 'center';
  const btn = Object.assign(document.createElement('button'), {
    textContent: 'Видалити',
    className: 'remove-button',
    onclick: () => ElementManager.remove(index)
  });
  td.appendChild(btn);
  return td;
}

const ElementManager = {
  handleAdd() {
    const select = getEl('elementSelect');
    const selectedId = select.value;
    if (!selectedId) return;
    const element = Elements.find(el => el.id === selectedId);
    if (!element || selectedElements.some(e => e.id === selectedId)) return;
    selectedElements.push({ ...element, quantity: 1 });
    renderTable();
    LossesCalculator.calculate();
    select.value = '';
  },
  handleSwitchChange(e) {
    sectionalSwitch = e.target.value;
    LossesCalculator.calculate();
  },
  updateQuantity(index, quantity) {
    if (selectedElements[index]) {
      selectedElements[index].quantity = quantity;
      LossesCalculator.calculate();
    }
  },
  remove(index) {
    selectedElements.splice(index, 1);
    renderTable();
    LossesCalculator.calculate();
  }
};

const ReliabilityCalculator = {
  calculateSystemReliability() {
    let OmegaSum = 0;
    let TB_Sum = 0;
    selectedElements.forEach(el => {
      OmegaSum += el.omega * el.quantity;
      TB_Sum += el.omega * el.tB * el.quantity;
    });
    const sectional = SectionalSwitches.find(sw => sw.id === sectionalSwitch);
    if (!sectional) return { omega: 0, tB: 0 };
    const omegaSystem = OmegaSum * sectional.kSection;
    const tB_System = TB_Sum / OmegaSum || 0;
    return { omega: omegaSystem, tB: tB_System };
  }
};

const LossesCalculator = {
  calculate() {
    const P1 = getInputValue('activePowerInput');
    const t1 = getInputValue('powerOffDurationInput');
    const { omega, tB } = ReliabilityCalculator.calculateSystemReliability();
    const deltaW = HOURS_IN_YEAR * omega * tB * P1;
    const Wv = HOURS_IN_YEAR * P1;
    const Kn = deltaW / Wv || 0;
    updateResultField('omegaSystem', omega);
    updateResultField('tB_System', tB);
    updateResultField('deltaW', deltaW);
    updateResultField('Kn', Kn, 4);
  }
};
function showSection(sectionId) {
	const sections = document.querySelectorAll('section');
	sections.forEach(section => {
	  section.classList.add('hidden');
	});
  
	const activeSection = document.getElementById(sectionId + 'Section');
	if (activeSection) {
	  activeSection.classList.remove('hidden');
	}
  }
  