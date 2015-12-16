#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include <util/atomic.h>
#include <stdbool.h>


#define		SetBit(reg, bit)		reg |= (1<<bit)		//Установить бит в "1" (bit) в регистре (reg),  не трогая все остальные
#define		ClearBit(reg, bit)		reg &= (~(1<<bit))	//Сбросить  бит (bit) в регистре (reg),  не трогая все остальные
#define		IsSetBit(reg, bit)		((reg & (1<<bit)) != 0)	//Проверка, установле ли  разряд (bit) в регистре (reg) ?
#define		IsClearBit(reg, bit)		((reg & (1<<bit)) == 0)	//Проверка, очищен ли разряд (bit) в регистре (reg) ?

// ==========================================
// 			Шаговый двигатель
// ==========================================
/* Определение регистров порта к которму подключен шаговый двигатель (ШД) */
#define		SM_PORT		PORTD		
#define		SM_PIN		PIND
#define		SM_DDR		DDRD

/* определения номеров портов к котрым подключены обмотки ШД */
#define		SM_WIRE_1	4
#define		SM_WIRE_2	5
#define		SM_WIRE_3	6
#define		SM_WIRE_4	7

// ==========================================
// 			Опто-прерыватель с открытым каналом 
// 			Датчик остановки шагового двигателя
// ==========================================
/* Определение регистров порта к которму подключен опто-прерыватель */
#define		OPTO_SENSOR_PORT	PORTB
#define		OPTO_SENSOR_DDR		DDRB
#define		OPTO_SENSOR_PIN		PINB

/* Номер порта к которому подключен опто-прерыватель*/
#define		OPTO_SENSOR		0

// ==========================================
// 			Led1 для логгирования
// ==========================================
/* определения регистров для logging led1 */
#define		STATE_LED_PORT		PORTB
#define		STATE_LED_DDR		DDRB

/* определения номера портв к которым подключен logging led1 */
#define 	STATE_LED 		1

// ==========================================
//			Вывод генерации меандра
// ==========================================
/* определения регистров */
#define		MEANDER_PORT		PORTB
#define		MEANDER_DDR			DDRB

/* определения номера порта на который выводиться меандр */
#define 	MEANDER 			2

// ==========================================
// 			Настройки ТС1, отвечающего за
//			вращение ШД
// ==========================================
/* Начальное значение регистра TCNT1, выбираеться в зависимости от скорости  вращения ШД */
#define 	TCNT1_SM_FORVARD_MOVE		64000 // срабатывание каждые ~ 13.3 ms
#define 	TCNT1_SM_REVERSE_MOVE		64380 // срабатывание каждые ~ 10.0 ms

/* Максимальное количество шагов в одном направлении. Зависит от длины максимального перемещения ротора в одном направлении */
// #define		SM_MAX_PROGREESS	300

// ==========================================
// 			Другие макросы
// ==========================================
/* Количество полных поворотов ротора ШД для перемещения каретки в червячной передачи на 1 см */
#define 	SM_FULL_ONE_ROTATE_1CM	9

/* Количество шагов ротора ШД при полном обороте на 360 град. (нормальный шаг)*/
#define 	SM_ONEROTATE_360_DEGR	95

/* Полная длина (см) на которую перемещаеться каретка, достаточная для полного измерения образца */
#define 	FULL_LENGTH_CM			6

/* Вычисление количества шагов ШД для перемещения каретки в червячной передачи на полную длину , при выполнении измерений образца */
#define		SM_FULL_MSR_STEPS		SM_FULL_ONE_ROTATE_1CM * SM_ONEROTATE_360_DEGR * FULL_LENGTH_CM


/* задержка между каждым шагом при врещении ШД, это определяет скорсть вращения. Используеться только для парковки в нач состояние ШД */
#define		SM_DELAY_STEP_MS	5


/* Выполнен ли захват ТС1, т.е. включен он (ШД вращаеться) или нет. false - ТС1 свободен (выключен), ШД остановлен; true - ТС1 включен, ШД выполняет вращение */
volatile bool isBlockTC1 = false;

// запоминае состояние состояния кнопки запуска/остановки процееса измерений
volatile bool btnStateFlag = false;

volatile unsigned char usartRxBuf = 0;	//однобайтный буфер
volatile unsigned char lowByte; // младший байт ацп преобразования
volatile unsigned int adcResult; // результат ацп 

// таблица шагов ШД , нормальный шаг
// const char smTableNormalStep[] = {_BV(SM_WIRE_1), _BV(SM_WIRE_2), _BV(SM_WIRE_3), _BV(SM_WIRE_4)};
const char smTableNormalStep[] = {_BV(SM_WIRE_4), _BV(SM_WIRE_3), _BV(SM_WIRE_2), _BV(SM_WIRE_1)};

//======================================
//	Команды от ПК
//	Каждая команда - массив символов
//======================================
char pcToMcuStartMeasureComm[] = {'a', 'q', 'l'};
char pcToMcuStopMeasureComm[] = {'a', 'b', 'k'};
char pcToMcuInitDevice[] = {'a', 'g', 'd'};
int commCount = 0; //общий счетчик для определения совпадения команды посимвольно 
/* Перечислегние команд от ПК */
enum fromPcCommands{
	STDBY,
	DO_START_SM,
	DO_STOP_SM
};
volatile enum fromPcCommands pcCommand = STDBY;



void initIO(void);
void initUSART(void);
void initADC(void);
void initExtInt0(void);
void initTC2(void);
void turnOnTC1(void);
void turnOffTC1(void);
void sendCharToUSART(unsigned char sym);
unsigned char getCharOfUSART(void);
bool checkSMInBeginPos(void);
void stopSM(void);
void blinkLed1r();
void blinkLed2r();



int main(void){
	cli();
	initIO();
	initUSART();
	initADC();
	initExtInt0();
	initTC2();
	sei();

	checkSMInBeginPos();

	ADCSRA |= (1<<ADSC); // запускаем первое АЦП преобразование

	unsigned char sym;

	while(1){
		sym = getCharOfUSART();
		
		//=====================================
		//	Блоки проверки соответствия команд от ПК
		//=====================================
		if (sym == pcToMcuInitDevice[commCount]){
			commCount ++;
			if (commCount == sizeof(pcToMcuInitDevice)){
				sendCharToUSART('g'); //ascii = 103
				_delay_ms(15);
				sendCharToUSART('h'); //ascii = 104
				_delay_ms(15);
				sendCharToUSART('y'); //ascii = 121
				_delay_ms(15);
				commCount = 0;
			}
		} else 
		if (sym == pcToMcuStartMeasureComm[commCount]){
			commCount ++;
			if (commCount == sizeof(pcToMcuStartMeasureComm)){
				pcCommand = DO_START_SM;
				commCount = 0;
			}
		} else if (sym == pcToMcuStopMeasureComm[commCount]){
			commCount ++;
			if (commCount == sizeof(pcToMcuStopMeasureComm)){
				pcCommand = DO_STOP_SM;
				commCount = 0;
			}
		}

		//=========================================
		//	Блоки выполнения методов в зависимости
		//	от полученной команды от ПК
		//=========================================
		switch(pcCommand){
			case STDBY:
				break;

			case DO_START_SM:
				if (isBlockTC1 == false){
					cli();
					turnOnTC1();
					sei();
				}
				break;

			case DO_STOP_SM:
				btnStateFlag = false;
				break;
		}

	}
}

// Прием символа по usart`у в буфер
ISR(USART_RXC_vect){ 
   usartRxBuf = UDR;  
} 

// Обработка прерывания от ацп
ISR(ADC_vect){
	// считываем младший и старший байты результата АЦ-преобразования и образуем из них 10-разрядный результат
	lowByte = ADCL;
	adcResult = (ADCH<<8)|lowByte;

	// запускаем новое АЦ-преобразование
	ADCSRA |= (1<<ADSC);
}

// Обработчик кнопка для запуска/остановки процееса измерений
ISR(INT0_vect){
	// start measuring
	if (btnStateFlag == false){
		btnStateFlag = true;
		pcCommand = DO_START_SM;
	// stop measuring
	} else if (btnStateFlag == true){
		btnStateFlag = false;
		pcCommand = DO_STOP_SM;
	}

	_delay_ms(200); // антидребизг
}

const unsigned char kSecFactor = 10;
volatile unsigned char kSecCount = 0;
volatile bool isSwithOnMeandr = false;
ISR(TIMER2_COMP_vect){
	if (kSecCount == kSecFactor - 1){
		kSecCount = 0;
		if (!isSwithOnMeandr){
			SetBit(MEANDER_PORT, MEANDER);
			isSwithOnMeandr = true;
		} else {
			ClearBit(MEANDER_PORT, MEANDER);
			isSwithOnMeandr = false;
		}
	} else {
		kSecCount ++;
	}
	
}

// константа определяющая количество шагов которое нужно пропустить 
// прежде чем отправить значение ацп с датчика освещенности.
// Это нужно для синхронизации количества шагов с еденицами измерения длины образца в мм.
// 95 * 9 = 855 - перемещение каретки на 1 см.  См. SM_FULL_ONE_ROTATE_1CM SM_ONEROTATE_360_DEGR
// Округлим это значение до 850, т.к. полученные данные приблеженные. 
// Далее 10 мм делим на 0.2 мм = 50 раз. В 50 раз нужно уменьшить полученное число шагов. 
// Т.е. 850 делим на 50 = 17 шагов нужно для перемещения червяка на 0.2 мм. 
// Это приблеженное значение т.к. SM_FULL_ONE_ROTATE_1CM SM_ONEROTATE_360_DEGR требуют калибровки!
const unsigned char stepAdcSyncConst = 17;

ISR(TIMER1_OVF_vect){
	// в каком направлении выполняеться движение ШД
	// false - прямое ; true - обратное
	static bool isDisableForward = false;
	// счетчик состяний обмоток ШД
	static signed char stepCount = 0;
	// счетчик шагов ШД в процессе измерения
	static int smProgressCount = 0;
	static unsigned char stepAdcSyncCount = 0;

	// прямой ход ШД - выполнение измерений
	if(smProgressCount < SM_FULL_MSR_STEPS && !isDisableForward){
		if (pcCommand == DO_START_SM){

			SM_PORT = smTableNormalStep[stepCount];
			stepCount ++;
			if (stepCount > 3) stepCount = 0;

			if (stepAdcSyncCount == stepAdcSyncConst){
				sendCharToUSART((unsigned char)(adcResult/4));
				stepAdcSyncCount = 0;
			} else {
				stepAdcSyncCount ++;
			}

			smProgressCount ++;

		}else if (pcCommand == DO_STOP_SM){
			isDisableForward = true;
			stepCount = 3;
		}
	}else if(!isDisableForward){
		isDisableForward = true;
		stepCount = 3;
		_delay_ms(300);
	}

	// обратный ход ШД - возвращение в исходное положение
	if (smProgressCount > 0 && isDisableForward){
		SM_PORT = smTableNormalStep[stepCount];
		stepCount --;
		if (stepCount < 0) stepCount = 3;

		smProgressCount --;
	}

	// останока ШД
	if (smProgressCount == 0 && isDisableForward){
		isDisableForward = false;
		stepCount = 0;
		smProgressCount = 0;
		turnOffTC1();
	}

	if (!isDisableForward){
		TCNT1 = TCNT1_SM_FORVARD_MOVE;
	} else {
		TCNT1 = TCNT1_SM_REVERSE_MOVE;
	}
}


/* инициализация портов в/в */
void initIO(void){
	/* Настприваем на ВЫХОД порты к которым подклчен двигатель */
	SetBit(SM_DDR, SM_WIRE_1);
	SetBit(SM_DDR, SM_WIRE_2);
	SetBit(SM_DDR, SM_WIRE_3);
	SetBit(SM_DDR, SM_WIRE_4);

	stopSM();

	/* Настриваем на ВХОД порт для подключения опто-прерывателя */
	ClearBit(OPTO_SENSOR_DDR, OPTO_SENSOR);
	
	/* Настриваем на ВЫХОД порт для подключения state led  */
	SetBit(STATE_LED_DDR, STATE_LED);

	/* Настриваем на ВЫХОД порт для меандра  */
	SetBit(MEANDER_DDR, MEANDER); 
}

void initUSART(){
	//UBRR=95 @ 9600 бод при 14,7456 MHz (U2X = 0)
	//UBRR=51 @ 9600 бод при 8 MHz (U2X = 0)
	// примерно 60 выб/с для 1 канала ???!!!
	UBRRH = 0;
	UBRRL = 51; 
	
	UCSRB=(1<<RXCIE)|(1<<RXEN)|(1<<TXEN); //разр. прерыв при приеме, разр приема, разр передачи.
	UCSRC=(1<<URSEL)|(1<<UCSZ1)|(1<<UCSZ0);  //размер слова 8 разрядов
}


// настройка АЦП
void initADC(void){
	ADCSRA |= (1<<ADPS2)|(1<<ADPS1)|(1<<ADPS0); // предделитель на 128
	ADCSRA |= (1<<ADIE);                        // разрешаем прерывание от ацп
	ADCSRA |= (1<<ADEN);                        // разрешаем работу АЦП

	ADMUX |= (1<<REFS0)|(1<<REFS1);             // работа от внутр. ИОН 2,56 В
	ADMUX|=(0<<MUX3)|(0<<MUX2)|(0<<MUX1)|(0<<MUX0);
}

// настройка прерывания от внешнего источник (кнопки)
void initExtInt0(){
	// срабатывание по низкому уровню на выводе INT0
	ClearBit(MCUCR, ISC00);
	ClearBit(MCUCR, ISC01);
	// резрещаем внешние прерывания
	SetBit(GICR, INT0);
}

// настройка ТС2 - генерации меандра 
void initTC2(){
	SetBit(TIMSK, OCIE2);
	SetBit(TCCR2, WGM21);
	SetBit(TCCR2, CS20);
	SetBit(TCCR2, CS21);
	SetBit(TCCR2, CS22);
	OCR2 = 195;
}


// Запуск ТС1 
void turnOnTC1(){
	isBlockTC1 = true;
	// разрешаем прерывания при переполнении
	SetBit(TIMSK, TOIE1);

	// устанавливаем делитель частоты 64
	SetBit(TCCR1B, CS10);
	SetBit(TCCR1B, CS11);
	ClearBit(TCCR1B, CS12);
	
	// устанавливаем нормальный режим
	// в этом режиме OCR1A нельзя изменить, OCR1A = 0xFFFF
	ClearBit(TCCR1A, WGM10);
	ClearBit(TCCR1A, WGM11);
	ClearBit(TCCR1B, WGM12);
	ClearBit(TCCR1B, WGM13);

	// подстраиваим частоту срабатываения
	TCNT1 = TCNT1_SM_FORVARD_MOVE;
}

//Отключаем ТС1
void turnOffTC1(){
	cli();
	// выключение таймера
	ClearBit(TCCR1B, CS10);
	ClearBit(TCCR1B, CS11);
	ClearBit(TCCR1B, CS12);

	stopSM();

	pcCommand = DO_STOP_SM;
	isBlockTC1 = false;
	sei();
}

// отправка символа по usart`у
void sendCharToUSART(unsigned char sym){
	while(!(UCSRA & (1<<UDRE)));
	UDR = sym;  
}

// чтение буфера usart
unsigned char getCharOfUSART(void){
	unsigned char tmp;
	ATOMIC_BLOCK(ATOMIC_FORCEON){
		tmp = usartRxBuf;
		usartRxBuf = 0;
	}
	return tmp;  
}



// Проверка расположения (парковки) ШД в исходном (начальном) положении 
// Обратный ход ШД до закрытия окна опто-прерывателя
bool checkSMInBeginPos(){
	cli();
	while (IsClearBit(OPTO_SENSOR_PIN, OPTO_SENSOR)){
		SM_PORT = smTableNormalStep[3];
		_delay_ms(SM_DELAY_STEP_MS);
	
		SM_PORT = smTableNormalStep[2];
		_delay_ms(SM_DELAY_STEP_MS);
		
		SM_PORT = smTableNormalStep[1];
		_delay_ms(SM_DELAY_STEP_MS);
		
		SM_PORT = smTableNormalStep[0];
		_delay_ms(SM_DELAY_STEP_MS);
	}
	stopSM();
	sei();
	return true;
}

// остановка ШД - снятия напряжения с управляющих выводов
void stopSM(){
	ClearBit(SM_PORT, SM_WIRE_1);
	ClearBit(SM_PORT, SM_WIRE_2);
	ClearBit(SM_PORT, SM_WIRE_3);
	ClearBit(SM_PORT, SM_WIRE_4);
}



void blinkLed1r(){
	SetBit(STATE_LED_PORT, STATE_LED);
	_delay_ms(100);
	ClearBit(STATE_LED_PORT, STATE_LED);
}

void blinkLed2r(){
	SetBit(STATE_LED_PORT, STATE_LED);
	_delay_ms(30);
	ClearBit(STATE_LED_PORT, STATE_LED);
	_delay_ms(30);
	SetBit(STATE_LED_PORT, STATE_LED);
	_delay_ms(30);
	ClearBit(STATE_LED_PORT, STATE_LED);
}