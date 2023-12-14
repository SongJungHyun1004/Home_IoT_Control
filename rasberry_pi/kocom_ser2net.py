#!/usr/bin/env python3
# -*- coding: utf-8 -*-

#===============================================================================
# 2019-06-09 by 따분 v1.0
# apt-get install mosquitto
# pip3 install httplib2
# pip3 install pyserial
# pip3 install paho-mqtt
#===============================================================================

import serial
import time
import platform
import threading
import queue
import json
import httplib2
import ssl
import paho.mqtt.client as mqtt
from binascii import unhexlify


MQTT_SERVER = '192.168.243.198'  #내부망 MQTT 서버주소


ENDPOINT = "a287snqybyvrg6-ats.iot.ap-northeast-2.amazonaws.com" #외부망 MQTT 서버주소
THING_NAME = 'pi' # AWS iot core 사물이름
CERTPATH =  "/home/pi/aws/0905511fd4696d741b6757fd3fd2ef05ad52cfb6173f15311d8737bd5c646e6d-certificate.pem.crt" # cert파일 경로
KEYPATH = "/home/pi/aws/0905511fd4696d741b6757fd3fd2ef05ad52cfb6173f15311d8737bd5c646e6d-private.pem.key" # key 파일 경로
CAROOTPATH = "/home/pi/aws/AmazonRootCA1.pem" # RootCaPem 파일 경로



os_platfrom = platform.system()
if os_platfrom == 'Linux':
    SERIAL_PORT1='/dev/ttyUSB1' #라즈베리파이 USB PORT 이름 확인
    SERIAL_PORT2='/dev/ttyUSB0'


def _connect_serial():
    ser = serial.Serial(SERIAL_PORT1, 9600)
    ser1 = serial.Serial(SERIAL_PORT2, 9600)
    ser.bytesize = 8
    ser.stopbits = 1
    ser.autoOpen = False
    #ser.encoding='hex'

    if ser.isOpen() and ser1.isOpen():
        print ('시리얼 정보 : {}, {}'.format(ser, ser1))
        return ser, ser1
    else:
        print ('소켓이 열려있지 않습니다.[{}, {}]'.format(ser, ser1))
        return False, False

ser, ser1 = _connect_serial()
BUF_SIZE = 10
q = queue.Queue(BUF_SIZE)
#q = queue.LifoQueue(BUF_SIZE)

# mqtt 구독/발행 ----------------------------
def on_message(mqttc, obj, msg):
    #print("수신 명령어::" + msg.topic + " " + str(msg.qos) + " " + msg.payload)
    command = msg.payload
    topic_d = msg.topic.split('/')
    print ('[command:{} topic:{}]'.format(command, topic_d))

    #토픽: homeassistant/light/light1/switch, homeassistant/light/light2/switch
    if 'homeassistant' in topic_d and 'light' in topic_d:
        print ('조명 : {}  액션 :{}'.format(topic_d[2], command))
        _light_act(ser, q, topic_d[2], command )
        # if command == b'ON':
        #     print('light on command')
        #     soc.send(bytearray.fromhex('aa5530bc0048000100001101400000000000870d0d'))
        # if command == b'OFF':
        #     print('light off command')
        #     soc.send(bytearray.fromhex('aa5530bc0048000100001101400000000000870d0d'))

    #토픽: homeassistnat/doorlock/switch
    if 'homeassistant' in topic_d and 'doorlock' in topic_d:
        print('도어락 액션 :{}'.format(command))
        _doorlock_act(ser1, command)
    #토픽: kocom/livingroom/light/1/command
    # if 'livingroom' in topic_d and 'light' in topic_d :
    #     print ('조명 : {}  액션 :{}'.format(topic_d[3], command ))
    #     _light_act(soc, q, 'light_' + topic_d[3], command )

    if 'state' in topic_d:
        print ('조명 1, 2 액션 :{}'.format(command))
        _light_search(ser, q)


def on_publish(mqttc, obj, mid):
    print("mid: " + str(mid))

def on_subscribe(mqttc, obj, mid, granted_qos):
    print("Subscribed: " + str(mid) + " " + str(granted_qos))

def on_log(mqttc, obj, level, log):
    print('LOG : {}'.format(log))

def on_connect(mqttc, obj, flags, rc):
    if rc == 0:
        msg =  "connected OK Returned code : "+ str(rc)
            # 튜플 멀티 구독
        sub_list=[("homeassistant/light/light1/switch", 0),("homeassistant/light/light2/switch", 0),("light/state", 0), ("homeassistant/doorlock/switch", 0)]
        mqttc.subscribe(sub_list)
    elif rc == 1:
        msg = "1: Connection refused – incorrect protocol version"
    elif rc == 2:
        msg = "2: Connection refused – invalid client identifier"
    elif rc == 3:
        msg = "3: Connection refused – server unavailable"
    elif rc == 4:
        msg = "4: Connection refused – bad username or password"
    elif rc == 5:
        msg = "5: Connection refused – not authorised"
    else:
        msg = rc
    print ('ReturnCode : [{}]'.format(msg))


# mqtt 시작
in_mqttc = mqtt.Client()
in_mqttc.on_connect = on_connect
in_mqttc.on_message = on_message
in_mqttc.on_subscribe = on_subscribe
in_mqttc.on_log = on_log
in_mqttc.username_pw_set("mqtt", "mqtt")
in_mqttc.connect(MQTT_SERVER, 1883, 60)
in_mqttc.loop_start()
# mqtt 구독/발행 ----------------------------끝

out_mqttc = mqtt.Client()
out_mqttc = mqtt.Client(client_id=THING_NAME)
out_mqttc.on_message = on_message
out_mqttc.on_connect = on_connect
out_mqttc.on_subscribe = on_subscribe
out_mqttc.tls_set(CAROOTPATH, certfile= CERTPATH, keyfile=KEYPATH, tls_version=ssl.PROTOCOL_TLSv1_2, ciphers=None)
out_mqttc.connect(ENDPOINT, port=8883)
out_mqttc.loop_start()


#스트링을 헥사데이터로 리스트화 하여 리턴
def _hex_payload_compose(hexdata):
    slide_windows = 2
    start = 0
    buf=[]
    for x in range(int(len(hexdata)/2)):
        buf.append('0x{}'.format(hexdata[start : slide_windows].lower() ))
        slide_windows += 2
        start += 2
    return buf

def _chk_sum(data):
    '''16진수 셋'''
    hex_list = _hex_payload_compose(data)
    sum_buf = 0
    for ix, x in enumerate(hex_list):
        #16진수를 10진수 로변환
        sum = int(x, 16)
        #print (chr(sum), end='' )
        if ix == 18:
            #16진수 0xee 소문자로 변환됨
            chksum_hex = hex((sum_buf % 256))
            #print ('18번째 데이터:[{}] 체크 환산값:[{}]'.format(hex_list[ix], chksum_hex))
            print(hex_list)
            if hex_list[ix] == chksum_hex:
                return (True, hex_list[ix] )
            else:
                return (False, hex_list[ix])
        #10진수를 16진수로 변환
        sum_buf += sum


def _make_chk_sum(payload):
    sum_buf = 0
    for ix, x in enumerate(payload):
        #16진수를 10진수 로변환
        sum = int(x, 16)
        sum_buf += sum
        if ix == 17:
            #전부 더한값에 mod 256에 1더하기하여 헥사값으로 출력
            chksum_hex ='{0:02x}'.format((sum_buf % 256))
            #print ('_make_chk_sum:{}'.format(chksum_hex))
    return chksum_hex


# 조명 조회
def _command_search(ser, device='000e', spot='0001'):
    prefix = 'aa55'
    act_type = '30bc'
    act_command = '003a'
    hex_end = '0d0d'
    # 전구 2개여서 2byte만 필요
    act_value = '0000'
    payload = prefix + act_type + device + spot + act_command + act_value + '000000000000'
    chk_sum =_make_chk_sum(_hex_payload_compose(payload))
    compose_send_data = payload + chk_sum + hex_end
    #print ('보내는데이터 재구성 : {}'.format(compose_send_data))
    ser.write(unhexlify(compose_send_data))
    #응답 대기를 위해 잠시 멈춤


def _doorlock_act(ser, command):
    # q_ret = _get_queue_data(q)
    ser.write(command)
    print(command)
    # for x in q_ret:
    #     ser.write(command)
    #     break

def _light_act(ser, q, target_light, onff ):
    #조명 조회
    _command_search(ser)
    time.sleep(3)
    #큐데이터 확보
    q_ret = _get_queue_data(q)
    #print ('큐데이터 리턴값{}'.format(q_ret))
    for x in q_ret:
        # 데이터 파싱
        p_ret=_data_parse(x)
        if p_ret['type'] =='30dc' and  p_ret['command'] =='003a' and p_ret['device'] == '000e':
            light_1 = p_ret['value'][:2]
            light_2 = p_ret['value'][2:4]
            print ('큐 조명상태 : {} {}'.format(light_1,light_2))
            if target_light == 'light1':
                value_join = ('ff' if onff == b'ON' else '00') + light_2
            if target_light == 'light2':
                value_join = light_1 + ('ff' if onff == b'ON' else '00')
            payload = p_ret['prefix'] + '30bc' + p_ret['device'] + p_ret['spot'] + '0000' + value_join + '000000000000'
            chk_sum =_make_chk_sum(_hex_payload_compose(payload))
            #print ('조명 명령어 페이로드 : {} 체크썸 : {}'.format(payload, chk_sum))
            ser.write(unhexlify(payload + chk_sum + '0d0d'))
            break

def _light_search(ser, q):
    #조명 조회만 진행
    _command_search(ser)
    time.sleep(3)
    #큐데이터 확보
    print("light searching...")
    q_ret = _get_queue_data(q)
    #print ('큐데이터 리턴값{}'.format(q_ret))
    for x in q_ret:
        # 데이터 파싱
        p_ret=_data_parse(x)
        if p_ret['type'] =='30dc' and  p_ret['command'] =='003a' and p_ret['device'] == '000e':
            light_1 = p_ret['value'][:2]
            light_2 = p_ret['value'][2:4]
            print ('큐 조명상태 : {} {}'.format(light_1,light_2))
            light = _light_parse(p_ret['value'])
            out_mqttc.publish("light/state1" , json.dumps(light))
            break


def _get_queue_data(q):
    buf = []
    while q.qsize():
        qdata = q.get()
        buf.append(qdata)
        #print ('queue data : {}'.format(qdata))
    #print(buf)
    return buf


def _light_parse(d_value):
    ret = {'light1': 'ON' if d_value[:2] == 'ff' else 'OFF',
    'light2': 'ON' if d_value[2:4] == 'ff' else 'OFF'}
    return ret


def _data_parse(hex_data):
    d_prefix = '{}'.format(hex_data[:4]) # aa55
    d_type = '{}'.format(hex_data[4:8])  # 30bc(요청) / 30dc(응답)
    d_spotordevice = '{}'.format(hex_data[8:12]) # 조명
    d_deviceorspot= '{}'.format(hex_data[12:16]) # 위치(방)
    d_command = '{}'.format(hex_data[16:20]) # 0000(제어)/003a(조회)
    d_value = '{}'.format(hex_data[20:36]) # value
    d_chksum = '{}'.format(hex_data[36:38]) # 체크썸
    d_end = '{}'.format(hex_data[38:42]) # end
    return {'prefix':d_prefix, 'type':d_type, 'device':d_spotordevice, 'spot':d_deviceorspot, 'command':d_command,
    'value':d_value, 'chksum':d_chksum, 'end':d_end,}

def _doorlock_parse(d_value):
    hex_ascii = ''
    str_ascii = ''
    num = 1
    for i in d_value:
        hex_ascii += i
        if num % 2 == 0:
            # 16진수 10진수 변환
            ascii = int(hex_ascii, 16)
            # 10진수 ascii코드로 변환
            str_ascii += chr(ascii)
            hex_ascii = ''
        num += 1
    return {'doorlock':str_ascii}


def _data_publish_state(hex_data, port):
    #==============================================================================
    # 30bc(요청) + 003a(조회) = 조회 명령어
    # 30bc(요청) + 0000(제어) = 제어 명령어 (turn of/on)
    # 30dc(응답) + 0000(제어) = 제어 명령어 대한 응답
    # 30dc(응답) + 003a(조회) = 조회 명령어에 대한 응답
    #==============================================================================
    if port == 0:
        d_prefix = '{}'.format(hex_data[:4])
        d_type = '{}'.format(hex_data[4:8])
        d_spotordevice = '{}'.format(hex_data[8:12])
        d_deviceorspot= '{}'.format(hex_data[12:16])
        d_command = '{}'.format(hex_data[16:20])
        d_value = '{}'.format(hex_data[20:36])
        d_chksum = '{}'.format(hex_data[36:38])
        d_end = '{}'.format(hex_data[38:42])
        #조명 상태
        if d_type == '30dc' and d_command =='0000' and d_spotordevice == '000e': #조명
            light = _light_parse(d_value)
            print ('[조명 상태] : 조명1:{} 조명2:{}'.format(light['light1'],light['light2']))
            in_mqttc.publish("homeassistant/light/status" , json.dumps(light))
            out_mqttc.publish("homeassistant/light/status" , json.dumps(light))
            print(json.dumps(light))
            print ('------------------------------------------------------')
    elif port == 1 or port == 2:
        if port == 1:
            d_doorlock = '{}'.format(hex_data[:8])
        if port == 2:
            d_doorlock = '{}'.format(hex_data[:12])
        doorlock = _doorlock_parse(d_doorlock)
        in_mqttc.publish("homeassistant/doorlock/status" , json.dumps(doorlock))
        out_mqttc.publish("homeassistant/doorlock/status" , json.dumps(doorlock))
        print(json.dumps(doorlock))
        print('-------------------------------')


#스레드 모듈
def _read_serial_data(q,ser):
    rev_buf = []
    start_flg = 0
    while True:
        if ser.readable():
            row_data = ser.read()
            ord_d = ord(row_data)
            hex_d = '{0:02x}'.format(ord_d)
            if hex_d == '41':
                start_flg = 1
            if start_flg == 1:
                rev_buf.append(row_data.decode().lower())

            if q.qsize() > 9:
                q.queue.clear()

            if len(rev_buf) > 42:
                logtime = time.strftime("%H:%M:%S",time.localtime())
                joindata = ''.join(rev_buf)
                chksum_rst = _chk_sum(joindata)
                #print (logtime, joindata,chksum_rst[0], chksum_rst[1])
                if chksum_rst[0]:
                    #print ('quing data : {}'.format(joindata))
                    q.put(joindata)
                    _data_publish_state(joindata, 0)
                rev_buf= []
                start_flg = 0

def _read_zigbee_data(ser1):
    rev_buf = []
    start_flg = 0
    while True:
        if ser1.readable():
            row_data = ser1.read()
            # if q.qsize() > 9:
            #     q.queue.clear()
            # q.put(row_data)
            ord_d = ord(row_data)
            hex_d = '{0:02x}'.format(ord_d)
            if hex_d == '4c' and start_flg == 0:
                start_flg = 1 # LOCK
            if hex_d == '55' and start_flg == 0:
                start_flg = 2 # UNLOCK
            if start_flg == 1:
                rev_buf.append(hex_d)
            if start_flg == 2:
                rev_buf.append(hex_d)
            if (len(rev_buf) == 4 and start_flg == 1) or (len(rev_buf) == 6 and start_flg == 2):
                joindata = ''.join(rev_buf)
                _data_publish_state(joindata, start_flg)
                rev_buf= []
                start_flg = 0




if __name__ == "__main__":
    t = threading.Thread(target=_read_serial_data, args=(q,ser))
    t.start()
    t1 = threading.Thread(target=_read_zigbee_data, args=(ser1,))
    t1.start()
