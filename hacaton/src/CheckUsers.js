import React from "react";
import { Alert, Flex, Spin, Switch } from 'antd';

import {styledComponents as S} from "./style/style";
import {FixedSizeList as List} from "react-window";
export default function CheckUsers() {
    const [users, setUsers] = React.useState([
        {name:'a b',cause:'dsada'},
        {name:'a b',cause:'dsada'},
        {name:'a b',cause:'dsada'},
        {name:'a b',cause:'dsada'},

    ]);
    const [loading, setLoading] = React.useState(false);
    const [buttonActive, setButtonActive] = React.useState(users.length!==0?true:false);
    const [url, setUrl] = React.useState('');



    const Row = ({ index, style, data }) => {
        const user = data[index];
        return (
            <S.CheckContainer style={style}>
                <div>
                    <p>{user.name}</p>
                    <p>{user.cause}</p>
                </div>
                <S.Button onClick={() => console.log('ban', user.name)}>Забанить</S.Button>
            </S.CheckContainer>
        );
    };

    return (
        <Flex gap="middle" vertical>
            <Spin spinning={loading} delay={500}>
                <label>Вы точно хотите запустить проверку всех пользователей это может занять определенное время для подтверждения введите ссылку на приглашения в ваш чат</label>
                <S.ButtonsInfo>
                    <input type={'url'} value={url} onChange={e => setUrl(e.target.value)} />
                    <S.Button style={{display:`${buttonActive?'none':''}`}} onClick={() => {
                        setButtonActive(!buttonActive)
                        setLoading(!loading)
                    }}>
                        да
                    </S.Button>
                </S.ButtonsInfo>
                <List
                    height={1000}
                    itemCount={users.length}
                    itemSize={100}
                    width="100%"
                    itemData={users}
                >{Row}</List>
            </Spin>
        </Flex>
    );
}