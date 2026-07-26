<#macro emailLayout>
<!doctype html>
<html lang="${locale.language}" dir="${(ltr)?then('ltr','rtl')}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PC-Verse</title>
    <style>
        body {
            width: 100% !important;
            margin: 0 !important;
            padding: 0 !important;
            background-color: #070b14;
            color: #e2e8f0;
            font-family: Arial, Helvetica, sans-serif;
            -webkit-text-size-adjust: 100%;
        }

        table {
            border-collapse: collapse;
        }

        .pcverse-shell {
            width: 100%;
            background-color: #070b14;
        }

        .pcverse-container {
            width: 100%;
            max-width: 600px;
        }

        .pcverse-brand {
            padding: 34px 24px 22px;
            color: #f8fafc;
            font-size: 26px;
            font-weight: 800;
            letter-spacing: 5px;
            text-align: center;
        }

        .pcverse-brand-accent {
            color: #38bdf8;
        }

        .pcverse-card {
            padding: 38px 42px;
            border: 1px solid #243044;
            border-top: 4px solid #3b82f6;
            border-radius: 16px;
            background-color: #0f172a;
            box-shadow: 0 24px 60px rgba(0, 0, 0, 0.28);
        }

        .pcverse-content,
        .pcverse-content p {
            margin-top: 0;
            color: #cbd5e1;
            font-size: 16px;
            line-height: 1.7;
        }

        .pcverse-content p {
            margin-bottom: 18px;
        }

        .pcverse-content a {
            display: inline-block;
            margin: 6px 0 10px;
            padding: 14px 24px;
            color: #ffffff !important;
            border-radius: 9px;
            background-color: #2563eb;
            font-weight: 700;
            line-height: 1.2;
            text-decoration: none;
        }

        .pcverse-footer {
            padding: 24px 20px 36px;
            color: #64748b;
            font-size: 12px;
            line-height: 1.6;
            text-align: center;
        }

        @media only screen and (max-width: 620px) {
            .pcverse-card {
                padding: 30px 22px !important;
                border-right: 0 !important;
                border-left: 0 !important;
                border-radius: 0 !important;
            }

            .pcverse-brand {
                padding-top: 26px !important;
                font-size: 22px !important;
            }
        }
    </style>
</head>
<body>
    <table role="presentation" class="pcverse-shell" width="100%" cellspacing="0" cellpadding="0" border="0">
        <tr>
            <td align="center">
                <table role="presentation" class="pcverse-container" width="600" cellspacing="0" cellpadding="0" border="0">
                    <tr>
                        <td class="pcverse-brand">
                            PC<span class="pcverse-brand-accent">-</span>VERSE
                        </td>
                    </tr>
                    <tr>
                        <td class="pcverse-card">
                            <div class="pcverse-content">
                                <#nested>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="pcverse-footer">
                            © ${.now?string('yyyy')} PC-Verse · ${msg("pcverseEmailFooter")}<br>
                            ${msg("pcverseEmailNoReply")}
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
</#macro>
