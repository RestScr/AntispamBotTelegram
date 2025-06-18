import styled from 'styled-components';
import { Link } from "react-router";
import { mainColor, borderColor, buttonColor, hoverColor, textColor } from '../config/config';

export const styledComponents = {
    Container: styled.div`
        max-width: 800px;
        margin: 0 auto;
        padding: 20px;
        font-family: sans-serif;
        background-color: ${mainColor};
        border: 1px solid ${borderColor};
        border-radius: 12px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    `,
    Header: styled.h1`
        margin-bottom: 24px;
        font-size: 30px;
        color: ${textColor};
    `,
    FilterItem: styled.div`
        font-family: sans-serif;

        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        padding: 12px;
        background-color: ${mainColor};
        border-radius: 4px;
        box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
    `,
    MenuContainer: styled.div`
          font-family: sans-serif;
        margin: 20px 0;
        border: 1px solid ${borderColor};
        border-radius: 4px;
    `,
    ListCard: styled.label`
        margin-top: 20px;
        color: ${textColor};
        
        font-size: 20px;
    `,
    ListItem: styled.div`
        padding: 12px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        text-align: center;
        border-bottom: 1px solid ${borderColor};
        border-radius: 16px;
    `,
    Button: styled.button`
        margin: 20px 0;
        width: 30%;
        height: 60px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 12px;
        background-color: ${buttonColor};
        color: ${textColor};
        padding: 14px;
        border: 1px solid ${buttonColor};
        cursor: pointer;
        transition: background-color 0.5s ease, border-color 0.3s ease;
        font-family: sans-serif;

        &:hover {
            background-color: ${hoverColor};
            border-color: ${hoverColor};
        }
    `,
    ModalContent: styled.div`
        padding: 20px;
    `,
    InformationCards: styled.div`
        display: flex;
        text-align: center;
        align-items: center;
        justify-content: space-around;
        border-radius: 10px;
        padding: 10px;
        background-color: ${mainColor};
        border: 1px solid ${borderColor};
    `,
    InformationCard: styled.div`
        display: flex;
        padding: 15px;
        width: 90px;
        font-size: 14px;
        height: 50px;
        background-color: ${mainColor};
        border-radius: 10px;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 8px;
        transition: background-color 0.5s ease, border-color 0.3s ease;

        &:hover {
            background-color: ${hoverColor};
            border-color: ${hoverColor};
        }
    `,
    Links: styled(Link)`
        text-decoration: none;
        color: ${textColor};
    `,
    ButtonsInfo: styled.div`
        display: flex;
        justify-content: space-between;
    `,
    ButtonLog: styled.button`
        margin: 20px 0;
        font-family: sans-serif;

        width: 20%;
        font-size: 16px;
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 12px;
        background-color: ${buttonColor};
        color: ${textColor};
        padding: 16px;
        border: 1px solid ${buttonColor};
        cursor: pointer;

        &:hover {
            background-color: ${hoverColor};
            border-color: ${hoverColor};
        }
    `,
    CheckContainer: styled.div`
        display: flex;
        flex-direction: row;
        align-items: center;
        justify-content: space-between;
        border-bottom: 4px solid ${borderColor};
    `

};
