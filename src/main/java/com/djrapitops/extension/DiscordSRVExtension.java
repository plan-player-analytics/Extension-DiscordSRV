/*
    Copyright(c) 2019 Risto Lahtela (Rsl1122)

    The MIT License(MIT)

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files(the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions :
    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/
package com.djrapitops.extension;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.FormatType;
import com.djrapitops.plan.extension.NotReadyException;
import com.djrapitops.plan.extension.annotation.*;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.core.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.core.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.core.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.core.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DataExtension for DiscordSRV.
 * <p>
 * Adapted from PluginData implementation by Vankka.
 *
 * @author Rsl1122
 */
@PluginInfo(name = "DiscordSRV", iconName = "discord", iconFamily = Family.BRAND, color = Color.CYAN)
public class DiscordSRVExtension implements DataExtension {

    @Override
    public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[]{
                CallEvents.PLAYER_JOIN, CallEvents.PLAYER_LEAVE,
                CallEvents.SERVER_EXTENSION_REGISTER, CallEvents.SERVER_PERIODICAL
        };
    }

    private void ensureDiscordSRVisReady() {
        if (!DiscordSRV.isReady) {
            throw new NotReadyException();
        }
    }

    private Optional<User> getDiscordUser(UUID playerUUID) {
        ensureDiscordSRVisReady();
        return Optional.ofNullable(DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(playerUUID))
                .map(DiscordUtil::getUserById);
    }

    private Optional<Guild> getMainGuild() {
        ensureDiscordSRVisReady();
        Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
        return Optional.ofNullable(mainGuild);
    }

    private Optional<Member> getMember(UUID playerUUID) {
        return getMainGuild().flatMap(guild -> getDiscordUser(playerUUID).map(guild::getMember));
    }

    private int getLinkedAccountCount() {
        ensureDiscordSRVisReady();
        return DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts().size();
    }

    private int getGuildMemberCount() {
        return getMainGuild().map(guild -> guild.getMembers().size()).orElse(-1);
    }

    private double calculatePercentage(double input1, double input2) {
        if (input1 == 0 || input2 == 0) {
            return 0.0;
        }

        return input1 / input2;
    }

    @StringProvider(
            text = "Warning!",
            description = "DiscordSRV is not ready",
            iconName = "exclamation-triangle",
            iconColor = Color.AMBER
    )
    public String notReadyNotice() {
        return "DiscordSRV isn't ready yet, no data is available.";
    }

    @BooleanProvider(
            text = "Has Linked Account",
            description = "Has the player linked their Discord account",
            priority = 101,
            conditionName = "hasLinkedAccount",
            iconName = "link",
            iconColor = Color.CYAN
    )
    public boolean hasLinkedAccount(UUID playerUUID) {
        return getDiscordUser(playerUUID).isPresent();
    }

    @Conditional("hasLinkedAccount")
    @StringProvider(
            text = "Username",
            description = "Linked Discord username of the player",
            priority = 100,
            iconName = "discord",
            iconFamily = Family.BRAND,
            iconColor = Color.CYAN,
            showInPlayerTable = true
    )
    public String username(UUID playerUUID) {
        return getDiscordUser(playerUUID).map(user -> '@' + user.getName() + '#' + user.getDiscriminator()).orElse("Not Linked");
    }

    @Conditional("hasLinkedAccount")
    @NumberProvider(
            text = "Account creation date",
            description = "When the Linked Discord account was created",
            priority = 99,
            iconName = "plus",
            iconColor = Color.BLUE,
            format = FormatType.DATE_YEAR
    )
    public long accountCreated(UUID playerUUID) {
        return getDiscordUser(playerUUID).map(user -> user.getCreationTime().toInstant().toEpochMilli()).orElse(-1L);
    }

    @Conditional("hasLinkedAccount")
    @BooleanProvider(
            text = "",
            hidden = true,
            conditionName = "hasMember"
    )
    public boolean hasMember(UUID playerUUID) {
        return getMember(playerUUID).isPresent();
    }

    @Conditional("hasMember")
    @StringProvider(
            text = "Nickname",
            description = "Nickname on the Guild",
            priority = 98,
            iconName = "user-ninja",
            iconColor = Color.ORANGE
    )
    public String nickName(UUID playerUUID) {
        return getMember(playerUUID).map(Member::getNickname).orElse("Not a Member");
    }

    @Conditional("hasMember")
    @NumberProvider(
            text = "Join Date",
            description = "When the Linked Discord account joined the Guild",
            priority = 97,
            iconName = "plus",
            iconColor = Color.GREEN,
            format = FormatType.DATE_YEAR
    )
    public long joinDate(UUID playerUUID) {
        return getMember(playerUUID).map(member -> member.getJoinDate().toInstant().toEpochMilli()).orElse(-1L);
    }

    @Conditional("hasMember")
    @StringProvider(
            text = "Roles",
            description = "Roles on the Guild",
            priority = 96,
            iconName = "user-circle",
            iconColor = Color.RED
    )
    public String roles(UUID playerUUID) {
        List<Role> roles = getMember(playerUUID).map(Member::getRoles).orElse(Collections.emptyList());

        if (roles.isEmpty()) {
            return "-";
        }

        StringBuilder roleBuilder = new StringBuilder();
        int size = roles.size();
        for (int i = 0; i < size; i++) {
            String roleName = roles.get(i).getName();
            roleBuilder.append(roleName);
            if (i < size - 1) {
                roleBuilder.append(", ");
            }
        }
        return roleBuilder.toString();
    }

    @NumberProvider(
            text = "Accounts Linked",
            description = "How many discord users have linked their player accounts.",
            priority = 100,
            iconName = "link",
            iconColor = Color.CYAN
    )
    public long accountsLinked() {
        return getLinkedAccountCount();
    }

    @NumberProvider(
            text = "Users in main guild",
            description = "How many discord users are on the main guild.",
            priority = 99,
            iconName = "users",
            iconColor = Color.CYAN
    )
    public long guildUsers() {
        return getGuildMemberCount();
    }

    @PercentageProvider(
            text = "Accounts linked / Users in main guild",
            description = "Percentage of users in guild who have linked their accounts.",
            priority = 97,
            iconName = "percentage",
            iconColor = Color.LIGHT_GREEN
    )
    public double accountsLinkedPerMembers() {
        return calculatePercentage(getLinkedAccountCount(), getGuildMemberCount());
    }
}