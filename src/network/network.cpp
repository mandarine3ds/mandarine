// Copyright 2017 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

#include "common/assert.h"
#include "common/logging/log.h"
#include "enet/enet.h"
#include "network/network.h"

namespace Network {

static std::shared_ptr<RoomMember> g_room_member; ///< RoomMember (Client) for network games
static std::shared_ptr<Room> g_room;              ///< Room (Server) for network games
static bool initialized = false;                  ///< Network initialization state
static bool in_room = false;                      ///< Track if we're currently in a room
// TODO(B3N30): Put these globals into a networking class

bool Init() {
    if (initialized) {
        return true;
    }

    if (enet_initialize() != 0) {
        return false;
    }

    g_room_member = std::make_shared<RoomMember>();
    g_room = std::make_shared<Room>();

    initialized = true;
    return true;
}

std::weak_ptr<Room> GetRoom() {
    return g_room;
}

std::weak_ptr<RoomMember> GetRoomMember() {
    return g_room_member;
}

void Shutdown() {
    if (!initialized || in_room) {
        return;
    }

    g_room_member.reset();
    g_room.reset();
    initialized = false;
    enet_deinitialize();
    LOG_INFO(Network, "Network shutdown complete");
}

void SetInRoom(bool status) {
    in_room = status;
}

} // namespace Network
